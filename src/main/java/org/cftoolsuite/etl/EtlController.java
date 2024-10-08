package org.cftoolsuite.etl;

import java.io.IOException;

import org.cftoolsuite.domain.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Profile({"advanced"})
@RestController
public class EtlController {

    private static final Logger log = LoggerFactory.getLogger(EtlController.class);

    private final GitRepositoryIngester ingester;

    public EtlController(GitRepositoryIngester ingester) {
        this.ingester = ingester;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody IngestRequest request) {
        try {
            ingester.ingest(request);
            return ResponseEntity.accepted().build();
        } catch (IOException e) {
            log.error("Trouble processing ingest request for Git repository", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
