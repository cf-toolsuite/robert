package org.cftoolsuite.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cdancy.bitbucket.rest.BitbucketApi;
import com.cdancy.bitbucket.rest.BitbucketClient;
import com.cdancy.bitbucket.rest.domain.common.Reference;
import com.cdancy.bitbucket.rest.domain.pullrequest.MinimalRepository;
import com.cdancy.bitbucket.rest.domain.pullrequest.ProjectKey;
import com.cdancy.bitbucket.rest.domain.pullrequest.PullRequest;
import com.cdancy.bitbucket.rest.options.CreatePullRequest;

@Component
public class BitBucketPullRequestClient implements PullRequestClient {

    private static Logger log = LoggerFactory.getLogger(BitBucketPullRequestClient.class);

    @Override
    public String pr(Repository localRepository, GitRequest request, String title, String body) {
        String result = null;
        if (request.pushToRemoteEnabled() && request.pullRequestEnabled()) {
            try {
                String bitbucketUrl = request.uri();
                String username = request.username();
                String password = request.password();

                BitbucketApi bitbucketApi = BitbucketClient.builder()
                    .endPoint(bitbucketUrl)
                    .credentials(username + ":" + password)
                    .build()
                    .api();

                String[] projectInfo = determineProjectInfo(getRemoteUrl(localRepository));
                String projectKey = projectInfo[0];
                String repoSlug = projectInfo[1];

                MinimalRepository repository = MinimalRepository.create(repoSlug, null, ProjectKey.create(projectKey));

                Reference fromRef = Reference.create(
                    "refs/heads/" + localRepository.getBranch(),
                    repository,
                    null,  // state
                    false,  // not a tag
                    localRepository.getBranch(),  // displayId
                    null  // latestCommit
                );

                Reference toRef = Reference.create(
                    "refs/heads/" + request.base(),
                    repository,
                    null,  // state
                    false,  // not a tag
                    request.base(),  // displayId
                    null  // latestCommit
                );

                CreatePullRequest createPR = CreatePullRequest.create(
                    title,
                    body,
                    fromRef,
                    toRef,
                    Collections.emptyList(),  // No reviewers
                    null  // No links
                );

                PullRequest pullRequest = bitbucketApi.pullRequestApi().create(projectKey, repoSlug, createPR);

                if (pullRequest != null) {
                    result = pullRequest.links().self().toString();
                    log.info("Pull request created: {}", result);
                } else {
                    log.error("Failed to create pull request");
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to create pull request", e);
            }
        } else {
            log.info("Pull request not enabled!");
        }
        return result;
    }

    private String[] determineProjectInfo(String remoteUrl) {
        String[] urlParts = remoteUrl.split("/");
        return new String[]{urlParts[urlParts.length - 2], urlParts[urlParts.length - 1].replace(".git", "")};
    }

    @Override
    public String uriPrefix() {
        return "https://bitbucket.org/";
    }
}
