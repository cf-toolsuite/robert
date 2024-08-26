package org.cftoolsuite.util;

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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class GitController {

    private static final Logger log = LoggerFactory.getLogger(GitController.class);

    private GitClient gitClient;
    private RefactoringService refactoringService;
    private PullRequestClientFactory pullRequestClientFactory;

    public GitController(
        GitClient gitClient,
        RefactoringService refactoringService,
        PullRequestClientFactory pullRequestClientFactory) {
        this.gitClient = gitClient;
        this.refactoringService = refactoringService;
        this.pullRequestClientFactory = pullRequestClientFactory;
    }

    @PostMapping("/clone")
    public void clone(@RequestBody GitSettings settings) {
        Repository repo = gitClient.getRepository(settings);
        try {
            gitClient.getLatestCommit(repo);
        } catch (GitAPIException | IOException e) {
            log.error("Trouble cloning Git repository", e);
        }
    }

    @PostMapping("/refactor")
    public void refactor(@RequestBody GitSettings settings, @Value("#{systemProperties['tpmDelay'] ?: '5'}") String delay) {
        Repository repo = gitClient.getRepository(settings);
        try {
            Map<String, String> sourceMap = null;
            Map<String, String> targetMap = new HashMap<>();
            if (StringUtils.isNotBlank(settings.commit())) {
                sourceMap = gitClient.readFiles(repo, settings.filePaths(), settings.commit());
            } else {
                sourceMap = gitClient.readFiles(repo, settings.filePaths());
            }
            log.info("Found {} files to refactor.", sourceMap.size());
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            sourceMap
                .entrySet()
                    .stream()
                        .forEach(entry -> {
                            executor.schedule(() -> {
                                log.info("-- Attempting to refactor {}", entry.getKey());
                                String refactoredValue = refactoringService.refactor(entry.getValue());
                                targetMap.put(entry.getKey(), refactoredValue);
                            }, Long.parseLong(delay), TimeUnit.SECONDS);
                        });
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
            String branchName = "refactor-" + UUID.randomUUID().toString();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String commitMessage = String.format("Refactored by %s on %s", refactoringService.getClass().getName(), LocalDateTime.now().format(formatter));
            gitClient.writeFiles(repo, targetMap, branchName, commitMessage);
            log.info("Refactoring completed on {}.", branchName);
            gitClient.push(settings, repo, branchName);
            pullRequestClientFactory.get(settings.uri()).pr(repo, settings, branchName, commitMessage);
        } catch (GitAPIException | IOException | InterruptedException e) {
            log.error("Trouble cloning Git repository", e);
        }
    }

}