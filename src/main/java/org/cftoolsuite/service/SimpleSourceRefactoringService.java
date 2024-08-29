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
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SimpleSourceRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSourceRefactoringService.class);

    private ApplicationContext context;
    private ChatClient chatClient;
    private GitClient gitClient;
    private String prompt;
    private String tpmDelay;
    private PullRequestClientFactory pullRequestClientFactory;

    public SimpleSourceRefactoringService(
        ApplicationContext context,
        ChatClient.Builder clientBuilder,
        GitClient gitClient,
        @Value("#{systemProperties['prompt'] ?: ''}") String prompt,
        @Value("classpath:/default.st") String defaultPrompt,
        @Value("#{systemProperties['tpmDelay'] ?: '5'}") String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory
    ) {
        this.context = context;
        this.chatClient = clientBuilder.build();
        this.gitClient = gitClient;
        this.prompt = StringUtils.isNotBlank(prompt) ? prompt : defaultPrompt;
        this.tpmDelay = tpmDelay;
        this.pullRequestClientFactory = pullRequestClientFactory;
        log.trace("Initializing {} with {}", SimpleSourceRefactoringService.class.getName(), this.prompt);
        if (!this.prompt.contains("{source}")) {
            log.error("Prompt must contain {source} placeholder!  Shutting down.");
            initiateShutdown(1);
        }
    }

    @Override
    public GitResponse refactor(GitRequest request) throws IOException {
        return refactor(request, this.tpmDelay);
    }

    protected String refactor(String source) {
        return
            chatClient
                .prompt()
				.user(u ->
                    u
                    .text(prompt)
                    .param("source", source)
                )
 				.call()
                .content();
    }

    protected GitResponse refactor(GitRequest request, String delay) throws IOException {
        Repository repo = gitClient.getRepository(request);
        Map<String, String> sourceMap = null;
        Map<String, String> targetMap = new HashMap<>();
        if (StringUtils.isNotBlank(request.commit())) {
            sourceMap = gitClient.readFiles(repo, request.filePaths(), request.commit());
        } else {
            sourceMap = gitClient.readFiles(repo, request.filePaths());
        }
        log.info("Found {} files to refactor.", sourceMap.size());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        sourceMap
            .entrySet()
                .stream()
                    .forEach(entry -> {
                        executor.schedule(() -> {
                            log.info("-- Attempting to refactor {}", entry.getKey());
                            String refactoredValue = refactor(entry.getValue());
                            targetMap.put(entry.getKey(), refactoredValue);
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

    private void initiateShutdown(int returnCode){
        SpringApplication.exit(this.context, () -> returnCode);
    }

}
