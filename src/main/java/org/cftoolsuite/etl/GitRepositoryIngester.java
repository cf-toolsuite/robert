package org.cftoolsuite.etl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.IngestRequest;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ListableBeanFactory;

public class GitRepositoryIngester {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryIngester.class);

    private VectorStore store;
    private GitClient client;
    private ListableBeanFactory beanFactory;

    public GitRepositoryIngester(VectorStore store, GitClient client, ListableBeanFactory beanFactory) {
        this.store = store;
        this.client = client;
        this.beanFactory = beanFactory;
    }

    public void ingest(IngestRequest request) throws IOException {
        GitRequest gitRequest = GitRequest.builder().uri(request.uri()).username(request.username()).password(request.password()).commit(request.commit()).build();
        Repository repo = client.getRepository(gitRequest);
        Map<String, String> fileMap = client.readFiles(repo, null, null, gitRequest.commit());
        String origin = client.getOrigin(repo);
        List<Document> documents =
            fileMap
                .entrySet()
                    .stream()
                        .map(entry -> {
                            log.info("-- Ingesting file: {}", entry.getKey());
                            Map<String, Object> customMetadata = Map.of("source", entry.getKey(), "charset", StandardCharsets.UTF_8, "origin", origin);
                            return new Document(entry.getValue(), customMetadata);
                        })
                        .collect(Collectors.toList());

        Map<String, DocumentTransformer> transformers = beanFactory.getBeansOfType(DocumentTransformer.class);
        if (transformers != null && !transformers.isEmpty()) {
            transformers.entrySet().forEach(entry -> entry.getValue().apply(documents));
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        store.accept(splitter.apply(documents));
    }
}
