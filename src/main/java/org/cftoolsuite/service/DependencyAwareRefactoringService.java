package org.cftoolsuite.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.cftoolsuite.domain.RefactoredSource;
import org.cftoolsuite.etl.GitRepositoryIngester;
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

    private static final Logger log = LoggerFactory.getLogger(SimpleSourceRefactoringService.class);

    private final ChatClient chatClient;
    private final String seek;
    private final String prompt;
    private final GitClient gitClient;
    private final PullRequestClientFactory pullRequestClientFactory;
    private final GitRepositoryIngester ingester;
    private final VectorStore store;

    public DependencyAwareRefactoringService(
        ChatClient chatClient,
        String seek,
        String prompt,
        GitClient gitClient,
        PullRequestClientFactory pullRequestClientFactory,
        GitRepositoryIngester ingester,
        VectorStore store
    ) {
        this.chatClient = chatClient;
        this.seek = seek;
        this.prompt = prompt;
        this.gitClient = gitClient;
        this.pullRequestClientFactory = pullRequestClientFactory;
        this.ingester = ingester;
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
        Repository repo = gitClient.getRepository(request);
        ingester.ingest(repo, request.commit());

        List<Document> candidates = CollectionUtils.isEmpty(request.filePaths()) ?
            store.similaritySearch(SearchRequest.query(seek).withTopK(100)) :
            store.similaritySearch(SearchRequest.query(seek).withFilterExpression(assembleFilterExpression(request)).withTopK(100));

        List<RefactoredSource> refactoredSources = refactor(candidates);
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

    protected List<RefactoredSource> refactor(List<Document> candidates) {
        String documents =
            candidates
                .stream()
                    .map(d -> String.format("filePath: %s\nsource: %s", d.getMetadata().get("source"), d.getContent()))
                    .collect(Collectors.joining("\n\n"));
        return chatClient
            .prompt()
            .user(u -> u.text(prompt)
            .param("documents", documents))
            .call()
            .entity(new ParameterizedTypeReference<List<RefactoredSource>>() {});
    }

    private Filter.Expression assembleFilterExpression(GitRequest request) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression result = null;
        if (!CollectionUtils.isEmpty(request.filePaths())) {
            List<String> slashSeparatedPaths = request.filePaths().stream()
                .filter(path -> path.contains("/") && !path.contains("."))
                .collect(Collectors.toList());

            List<String> convertedPackageNames = request.filePaths().stream()
                .filter(path -> path.contains(".") && !path.contains("/"))
                .map(pkg -> "src/main/java/" + pkg.replace('.', '/'))
                .collect(Collectors.toList());

            result = b.or(b.in("source", slashSeparatedPaths), b.in("source", convertedPackageNames)).build();
        }
        return result;
    }
}
