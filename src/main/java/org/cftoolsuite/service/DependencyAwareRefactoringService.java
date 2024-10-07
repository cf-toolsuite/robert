package org.cftoolsuite.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.cftoolsuite.domain.RefactoredSource;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;

public class DependencyAwareRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(DependencyAwareRefactoringService.class);

    private final ChatClient chatClient;
    private final String seek;
    private final String prompt;
    private final GitClient gitClient;
    private final PullRequestClientFactory pullRequestClientFactory;
    private final VectorStore store;

    public DependencyAwareRefactoringService(
        ChatClient chatClient,
        String seek,
        String prompt,
        GitClient gitClient,
        PullRequestClientFactory pullRequestClientFactory,
        VectorStore store
    ) {
        this.chatClient = chatClient;
        this.seek = seek;
        this.prompt = prompt;
        this.gitClient = gitClient;
        this.pullRequestClientFactory = pullRequestClientFactory;
        this.store = store;
    }

    @Override
    public GitResponse apply(GitRequest request) {
        try {
            return refactor(request);
        } catch (IOException e) {
            throw new GitOperationException("Error during refactoring", e);
        }
    }

    protected GitResponse refactor(GitRequest request) throws IOException {
        String prompt = String.join(System.lineSeparator() + System.lineSeparator(), "Discovery prompt:", request.discoveryPrompt(), "Refactor prompt:", request.refactorPrompt());
        Repository repo = gitClient.getRepository(request);
        List<Document> candidates = search(repo, request);

        if (CollectionUtils.isEmpty(candidates)) {
            log.info("No candidates found for refactoring.");
            return new GitResponse(prompt, request.uri(), null, null, Collections.emptySet());
        }

        List<RefactoredSource> refactoredSources = refactor(request.refactorPrompt(), candidates);
        Map<String, String> targetMap =
        refactoredSources
                .stream()
                .collect(Collectors.toMap(RefactoredSource::filePath, RefactoredSource::content));

        String branchName = "refactor-" + UUID.randomUUID().toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String commitMessage = String.format("Refactored by %s on %s", SimpleSourceRefactoringService.class.getName(), LocalDateTime.now().format(formatter));

        gitClient.writeFiles(repo, targetMap, branchName, commitMessage);
        log.info("Refactoring completed on {}.", branchName);
        gitClient.push(request, repo, branchName);
        String pullRequestUrl = pullRequestClientFactory.get(request.uri()).pr(repo, request, branchName, commitMessage);

        return new GitResponse(prompt, request.uri(), branchName, pullRequestUrl, targetMap.keySet());
    }

    // ALERT: Open issues in spring-ai to watch
    // @see https://github.com/spring-projects/spring-ai/issues/328
    // @see https://github.com/spring-projects/spring-ai/pull/1227
    protected List<Document> search(Repository repo, GitRequest request) throws IOException {
        String origin = gitClient.getOrigin(repo);
        String latestCommit = gitClient.getLatestCommit(repo).getId().name();
        String query = seek.replace("{discoveryPrompt}", request.discoveryPrompt());
        List<Document> candidates = store.similaritySearch(SearchRequest.query(query).withFilterExpression(assembleFilterExpression(request, origin, latestCommit)).withTopK(250));;
        log.trace("Refactor candidates are: {}", candidates);
        return candidates;
    }

    protected List<RefactoredSource> refactor(String articulation, List<Document> candidates) {
        String documents =
            candidates
                .stream()
                    .map(d -> String.format("filePath: %s%ncontent: %s", d.getMetadata().get("source"), d.getContent()))
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        return
            chatClient
                .prompt()
                .user(
                    u -> u  .text(prompt)
                            .param("refactorPrompt", articulation)
                            .param("documents", documents)
                )
                .call()
                .entity(new ParameterizedTypeReference<List<RefactoredSource>>() {});
    }

    private Filter.Expression assembleFilterExpression(GitRequest request, String origin, String latestCommit) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        String commit = StringUtils.isBlank(request.commit()) ? latestCommit : request.commit();
        Object[] fileExtensions = request.allowedExtensions().toArray(String[]::new);
        return
            b.and(
                b.and(
                    b.eq("commit", commit), b.eq("origin", origin)
                ),
                b.in("file-extension", fileExtensions)
            )
            .build();
    }
}
