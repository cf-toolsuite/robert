package org.cftoolsuite.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

public class SimpleSourceRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSourceRefactoringService.class);

    private final ChatClient chatClient;
    private final String prompt;
    private final GitClient gitClient;
    private final String tpmDelay;
    private final PullRequestClientFactory pullRequestClientFactory;

    public SimpleSourceRefactoringService(
        ChatClient.Builder clientBuilder,
        String prompt,
        GitClient gitClient,
        String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory
    ) {
        this.chatClient = clientBuilder.build();
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
        Map<String, String> sourceMap = StringUtils.isNotBlank(request.commit()) ?
            gitClient.readFiles(repo, request.filePaths(), request.commit()) :
            gitClient.readFiles(repo, request.filePaths());

        log.info("Found {} files to refactor.", sourceMap.size());
        Map<String, String> targetMap = new HashMap<>();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        sourceMap.forEach((key, value) -> {
            executor.schedule(() -> {
                log.info("-- Attempting to refactor {}", key);
                String refactoredValue = refactorSource(value);
                targetMap.put(key, refactoredValue);
            }, Long.parseLong(delay), TimeUnit.SECONDS);
        });

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IOException("Refactoring was interrupted.", e);
        }

        String branchName = "refactor-" + UUID.randomUUID().toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String commitMessage = String.format("Refactored by %s on %s", SimpleSourceRefactoringService.class.getName(), LocalDateTime.now().format(formatter));

        gitClient.writeFiles(repo, targetMap, branchName, commitMessage);
        log.info("Refactoring completed on {}.", branchName);
        gitClient.push(request, repo, branchName);
        String pullRequestUrl = pullRequestClientFactory.get(request.uri()).pr(repo, request, branchName, commitMessage);

        return new GitResponse(request.uri(), branchName, pullRequestUrl, targetMap.keySet());
    }

    protected String refactorSource(String source) {
        return chatClient
            .prompt()
            .user(u -> u.text(prompt)
            .param("source", source))
            .call()
            .content();
    }
}
