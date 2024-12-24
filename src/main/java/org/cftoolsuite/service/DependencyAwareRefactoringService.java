package org.cftoolsuite.service;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.FileSource;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DependencyAwareRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(DependencyAwareRefactoringService.class);

    private final ChatClient chatClient;
    private final String seek;
    private final String prompt;
    private final String dependenciesManagementStanza;
    private final GitClient gitClient;
    private final String tpmDelay;
    private final PullRequestClientFactory pullRequestClientFactory;
    private final VectorStore store;

    public DependencyAwareRefactoringService(
        ChatClient chatClient,
        String seek,
        String prompt,
        String dependenciesManagementStanza,
        GitClient gitClient,
        String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory,
        VectorStore store
    ) {
        this.chatClient = chatClient;
        this.seek = seek;
        this.prompt = prompt;
        this.dependenciesManagementStanza = dependenciesManagementStanza;
        this.gitClient = gitClient;
        this.tpmDelay = tpmDelay;
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
        // Step 1: Search for candidates to refactor matching the discovery prompt
        Map<String, String> sourceContentMap =
            search(repo, request)
                .stream()
                .collect(Collectors.groupingBy(
                    d -> (String) d.getMetadata().get("source"),
                    Collectors.mapping(Document::getText, Collectors.joining("\n"))
                ));

        Set<FileSource> candidates =
            sourceContentMap.entrySet().stream()
                .map(entry -> new FileSource(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(candidates)) {
            log.info("No candidates found for refactoring.");
            return new GitResponse(prompt, request.uri(), null, null, Collections.emptySet());
        }

        // Step 2: Refactor without taking dependencies into account
        Set<FileSource> refactoredSourcesWithoutTakingDependenciesIntoAccount = refactor(request.refactorPrompt(), candidates, "");
        Set<FileSource> refactoredSources = new HashSet<>(refactoredSourcesWithoutTakingDependenciesIntoAccount);

        // Step 3: Refactor taking dependencies into account
        Map<String, String> potentialDependencies = gitClient.readFiles(repo, null, request.allowedExtensions(), request.commit());
        for (FileSource fs : refactoredSourcesWithoutTakingDependenciesIntoAccount) {
            for (Map.Entry<String, String> entry : potentialDependencies.entrySet()) {
                Set<FileSource> pairs = Set.of(fs, new FileSource(entry.getKey(), entry.getValue()));
                refactoredSources.addAll(refactor(request.refactorPrompt(), pairs, this.dependenciesManagementStanza));
            }
        }

        Map<String, String> targetMap =
        refactoredSources
                .stream()
                .collect(Collectors.toMap(FileSource::filePath, FileSource::content));

        return completeRefactor(gitClient, pullRequestClientFactory, request, repo, targetMap, prompt);
    }

    // ALERT: Open issues in spring-ai to watch
    // @see https://github.com/spring-projects/spring-ai/issues/328
    // @see https://github.com/spring-projects/spring-ai/pull/1227
    protected List<Document> search(Repository repo, GitRequest request) throws IOException {
        String origin = gitClient.getOrigin(repo);
        String latestCommit = gitClient.getLatestCommit(repo).getId().name();
        String query = seek.replace("{discoveryPrompt}", request.discoveryPrompt());
        List<Document> candidates =
            store
                .similaritySearch(
                    SearchRequest
                        .builder()
                        .query(query)
                        .filterExpression(
                            SearchAssistant.assembleFilterExpression(request, origin, latestCommit)
                        )
                        .similarityThresholdAll()
                        .topK(100)
                        .build()
                );
        log.trace("Refactor candidates are: {}", candidates);
        return candidates;
    }

    protected Set<FileSource> refactor(String articulation, Set<FileSource> candidates, String dependenciesManagementStanza) throws IOException{
        Set<FileSource> refactoredSources = new HashSet<>();
        try (ScheduledExecutorService executor = Executors.newScheduledThreadPool(1)) {
            candidates.forEach(d -> {
                executor.schedule(() -> {
                        String files = String.format("filePath: %s%ncontent: %s", d.filePath(), d.content());
                        log.info("-- Attempting to refactor {}", files);
                        refactoredSources.addAll(refactorSource(articulation, dependenciesManagementStanza, files));
                }, Long.parseLong(this.tpmDelay), TimeUnit.SECONDS);
            });
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new IOException("Refactoring was interrupted.", e);
            }
        }
        return refactoredSources;
    }

    protected List<FileSource> refactorSource(String articulation, String dependencyManagementStanza, String documents) {
        return chatClient
            .prompt()
            .user(
                u -> u  .text(prompt)
                        .param("refactorPrompt", articulation)
                        .param("dependenciesManagementStanza", dependenciesManagementStanza)
                        .param("documents", documents)
            )
            .call()
            .entity(new ParameterizedTypeReference<List<FileSource>>() {});
    }

}
