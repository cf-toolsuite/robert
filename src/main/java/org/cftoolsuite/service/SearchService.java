package org.cftoolsuite.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.GitOperationException;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.CollectionUtils;

public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final GitClient gitClient;
    private final VectorStore store;

    public SearchService(VectorStore store, GitClient gitClient) {
        this.gitClient = gitClient;
        this.store = store;
    }

    public String readFile(GitRequest request) {
        try {
            String filePath = (String) request.filePaths().iterator().next();
            log.debug("Reading file: {}", filePath);
            Repository repo = gitClient.getRepository(request);
            String commitToUse = StringUtils.defaultIfBlank(request.commit(), gitClient.getLatestCommit(repo).name());
            Map<String, String> result = gitClient.readFile(repo, filePath, commitToUse);
            String contents = result.values().iterator().next();
            return contents;
        } catch (IOException ioe) {
            throw new GitOperationException("Error reading file", ioe);
        } catch (IllegalArgumentException iae) {
            throw new GitOperationException("Error processing read request", iae);
        }
    }

    public GitResponse search(GitRequest request) {
        try {
            return new GitResponse(request.discoveryPrompt(), request.uri(), request.commit(), null, doSearch(request));
        } catch (IOException e) {
            throw new GitOperationException("Error during search", e);
        }
    }

    // ALERT: Open issues in spring-ai to watch
    // @see https://github.com/spring-projects/spring-ai/issues/328
    // @see https://github.com/spring-projects/spring-ai/pull/1227
    protected Set<String> doSearch(GitRequest request) throws IOException {
        Repository repo = gitClient.getRepository(request);
        String origin = gitClient.getOrigin(repo);
        String latestCommit = gitClient.getLatestCommit(repo).getId().name();
        List<Document> candidates =
            store
                .similaritySearch(
                    SearchRequest
                        .query(request.discoveryPrompt())
                        .withFilterExpression(
                            assembleFilterExpression(request, origin, latestCommit)
                        )
                        .withSimilarityThresholdAll()
                        .withTopK(100)
                );
        log.trace("Search candidates are: {}", candidates);
        return candidates
                .stream()
                    .map(d -> (String) d.getMetadata().get("source"))
                    .collect(Collectors.toSet());
    }

    private Filter.Expression assembleFilterExpression(GitRequest request, String origin, String latestCommit) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        String commit = StringUtils.isBlank(request.commit()) ? latestCommit : request.commit();
        if (CollectionUtils.isEmpty(request.allowedExtensions())) {
            return b.and(b.eq("commit", commit), b.eq("origin", origin)).build();
        }
        Object[] fileExtensions = request.allowedExtensions().toArray(String[]::new);
        return
            b.and(
                b.and(
                    b.eq("commit", commit), b.eq("origin", origin)
                ),
                b.in("file-extension", fileExtensions)
            )
            .build();
    }
}
