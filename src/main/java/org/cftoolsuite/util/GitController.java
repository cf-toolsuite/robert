package org.cftoolsuite.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class GitController {

    private static final Logger log = LoggerFactory.getLogger(GitController.class);

    private GitClient client;
    private RefactoringService service;

    public GitController(
        GitClient client,
        RefactoringService service) {
        this.client = client;
        this.service = service;
    }

    @PostMapping("/clone")
    public void clone(@RequestBody GitSettings settings) {
        Repository repo = client.getRepository(settings);
        try {
            client.getLatestCommit(repo);
        } catch (GitAPIException | IOException e) {
            log.error("Trouble cloning Git repository", e);
        }
    }

    @PostMapping("/refactor")
    public void refactor(@RequestBody GitSettings settings) {
        Repository repo = client.getRepository(settings);
        try {
            Map<String, String> sourceMap = null;
            Map<String, String> targetMap = new HashMap<>();
            if (StringUtils.isNotBlank(settings.commit())) {
                sourceMap = client.readFiles(repo, settings.filePaths(), settings.commit());
            } else {
                sourceMap = client.readFiles(repo, settings.filePaths());
            }
            log.info("Found {} files to refactor.", sourceMap.size());
            for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                log.info("-- Attempting to refactor {}", entry.getKey());
                targetMap.put(entry.getKey(), service.refactor(entry.getValue()));
            }
            String branchName = "refactor-" + UUID.randomUUID().toString();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String commitMessage = String.format("Refactored by %s on %s", service.getClass().getName(), LocalDateTime.now().format(formatter));
            client.writeFiles(repo, targetMap, branchName, commitMessage);
            log.info("Refactoring completed on {}.", branchName);
            client.push(settings, repo, branchName);
        } catch (GitAPIException | IOException e) {
            log.error("Trouble cloning Git repository", e);
        }
    }

}