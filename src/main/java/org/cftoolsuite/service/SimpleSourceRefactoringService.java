package org.cftoolsuite.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.CollectionUtils;

public class SimpleSourceRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSourceRefactoringService.class);

    private final ChatClient chatClient;
    private final String prompt;
    private final GitClient gitClient;
    private final String tpmDelay;
    private final PullRequestClientFactory pullRequestClientFactory;

    public SimpleSourceRefactoringService(
        ChatClient chatClient,
        String prompt,
        GitClient gitClient,
        String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory
    ) {
        this.chatClient = chatClient;
        this.prompt = prompt;
        this.gitClient = gitClient;
        this.tpmDelay = tpmDelay;
        this.pullRequestClientFactory = pullRequestClientFactory;
    }

    @Override
    public GitResponse apply(GitRequest request) {
        try {
            return refactor(request, this.tpmDelay);
        } catch (IOException e) {
            throw new GitOperationException("Error during refactoring", e);
        }
    }

    protected GitResponse refactor(GitRequest request, String delay) throws IOException {
        Repository repo = gitClient.getRepository(request);
        Map<String, String> sourceMap = gitClient.readFiles(repo, request.filePaths(), request.allowedExtensions(), request.commit());

        log.info("Found {} files to refactor.", sourceMap.size());

        if (CollectionUtils.isEmpty(sourceMap)) {
            log.info("No candidates found for refactoring.");
            return new GitResponse(prompt, request.uri(), null, null, Collections.emptySet());
        }

        Map<String, String> targetMap = new HashMap<>();

        try (ScheduledExecutorService executor = Executors.newScheduledThreadPool(1)) {
            sourceMap.forEach((key, value) -> {
                executor.schedule(() -> {
                    log.info("-- Attempting to refactor {}", key);
                    String refactoredValue = refactorSource(request.refactorPrompt(), value);
                    targetMap.put(key, refactoredValue);
                }, Long.parseLong(delay), TimeUnit.SECONDS);
            });

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new IOException("Refactoring was interrupted.", e);
            }
        }

        return completeRefactor(gitClient, pullRequestClientFactory, request, repo, targetMap, prompt);
    }

    protected String refactorSource(String articulation, String source) {
        return chatClient
            .prompt()
            .user(
                u -> u  .text(prompt)
                        .param("refactorPrompt", articulation)
                        .param("source", source)
            )
            .call()
            .content();
    }
}
