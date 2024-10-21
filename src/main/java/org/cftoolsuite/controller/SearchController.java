package org.cftoolsuite.controller;

import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.cftoolsuite.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@Profile("advanced")
@RestController
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
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

    @PostMapping(value = "/fetch", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> fetch(@RequestBody GitRequest request) {
        try {
            String response = searchService.readFile(request);
            return ResponseEntity.ok(response);
        } catch (GitOperationException e) {
            log.error("Trouble processing fetch request for Git repository", e);
            return ResponseEntity.badRequest().body(String.format("uri: %s%nfilePath: %s", request.uri(), request.filePaths().iterator().next()));
        }
    }

}
