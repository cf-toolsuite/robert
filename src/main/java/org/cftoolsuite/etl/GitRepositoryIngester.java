package org.cftoolsuite.etl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
        GitRequest gitRequest =
            GitRequest
                .builder()
                .uri(request.uri())
                .username(request.username())
                .password(request.password())
                .commit(request.commit())
                .allowedExtensions(request.allowedExtensions())
                .build();
        Repository repo = client.getRepository(gitRequest);
        String latestCommit = client.getLatestCommit(repo).getId().name();
        Map<String, String> fileMap = client.readFiles(repo, null, gitRequest.allowedExtensions(), gitRequest.commit());
        String origin = client.getOrigin(repo);
        List<Document> documents =
            fileMap
                .entrySet()
                    .stream()
                        .map(entry -> {
                            log.info("-- Ingesting file: {}", entry.getKey());
                            Map<String, Object> customMetadata =
                                Map.of(
                                    "origin", origin,
                                    "commit", StringUtils.isBlank(gitRequest.commit()) ? latestCommit : gitRequest.commit(),
                                    "source", entry.getKey(),
                                    "file-extension", entry.getKey().contains(".") ? entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1): "",
                                    "charset", StandardCharsets.UTF_8
                                );
                            return new Document(entry.getValue(), customMetadata);
                        })
                        .collect(Collectors.toList());

        Map<String, DocumentTransformer> transformers = beanFactory.getBeansOfType(DocumentTransformer.class);
        if (!transformers.isEmpty()) {
            transformers.forEach((k, v) -> v.apply(documents));
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        store.accept(splitter.apply(documents));
    }
}
