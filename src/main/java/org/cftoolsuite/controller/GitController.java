package org.cftoolsuite.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.config.AllowedLanguageFileExtensions;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.cftoolsuite.domain.LanguageExtensions;
import org.cftoolsuite.service.RefactoringService;
import org.cftoolsuite.service.SearchService;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class GitController {

    private static final Logger log = LoggerFactory.getLogger(GitController.class);

    private final GitClient gitClient;
    private final RefactoringService refactoringService;
    private final SearchService searchService;
    private final AllowedLanguageFileExtensions config;

    public GitController(GitClient gitClient, RefactoringService refactoringService, SearchService searchService, AllowedLanguageFileExtensions config) {
        this.gitClient = gitClient;
        this.refactoringService = refactoringService;
        this.searchService = searchService;
        this.config = config;
    }

    @PostMapping("/clone")
    public void clone(@RequestBody GitRequest request) {
        Repository repo = gitClient.getRepository(request);
        gitClient.getLatestCommit(repo);
    }

    @PostMapping("/search")
    public ResponseEntity<GitResponse> search(@RequestBody GitRequest request) {
        try {
            GitResponse response = searchService.search(request);
            return ResponseEntity.ok(response);
        } catch (GitOperationException e) {
            log.error("Trouble processing search request for Git repository", e);
            return ResponseEntity.badRequest().body(GitResponse.noneFor(request.uri()));
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<String> fetch(@RequestBody GitRequest request) {
        try {
            String response = searchService.readFile(request);
            return ResponseEntity.ok(response);
        } catch (GitOperationException e) {
            log.error("Trouble processing fetch request for Git repository", e);
            return ResponseEntity.badRequest().body(String.format("uri: %s%nfilePath: %s", request.uri(), request.filePaths().iterator().next()));
        }
    }

    @PostMapping("/refactor")
    public ResponseEntity<GitResponse> refactor(@RequestBody GitRequest request) {
        try {
            GitResponse response = refactoringService.apply(request);
            return ResponseEntity.ok(response);
        } catch (GitOperationException e) {
            log.error("Trouble processing refactor request for Git repository", e);
            return ResponseEntity.badRequest().body(GitResponse.noneFor(request.uri()));
        }
    }

    @GetMapping("/language-extensions")
    public ResponseEntity<Set<LanguageExtensions>> languageExtensions() {
        return ResponseEntity.ok(config.getAllowedExtensions().entrySet().stream()
            .map(entry -> new LanguageExtensions(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet()));
    }
}
