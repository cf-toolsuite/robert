package org.cftoolsuite.controller;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.cftoolsuite.service.RefactoringService;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitController {

    private static final Logger log = LoggerFactory.getLogger(GitController.class);

    private final GitClient gitClient;
    private final RefactoringService refactoringService;

    public GitController(GitClient gitClient, RefactoringService refactoringService) {
        this.gitClient = gitClient;
        this.refactoringService = refactoringService;
    }

    @PostMapping("/clone")
    public void clone(@RequestBody GitRequest request) {
        Repository repo = gitClient.getRepository(request);
        gitClient.getLatestCommit(repo);
    }

    @PostMapping("/refactor")
    public ResponseEntity<GitResponse> refactor(@RequestBody GitRequest request) {
        try {
            GitResponse response = refactoringService.apply(request);
            return ResponseEntity.ok(response);
        } catch (GitOperationException e) {
            log.error("Trouble processing refactor request for Git repository", e);
            return ResponseEntity.unprocessableEntity().body(GitResponse.noneFor(request.uri()));
        }
    }
}
