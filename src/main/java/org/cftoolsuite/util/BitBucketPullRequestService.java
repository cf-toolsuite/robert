package org.cftoolsuite.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cdancy.bitbucket.rest.BitbucketApi;
import com.cdancy.bitbucket.rest.BitbucketClient;
import com.cdancy.bitbucket.rest.domain.common.Reference;
import com.cdancy.bitbucket.rest.domain.pullrequest.MinimalRepository;
import com.cdancy.bitbucket.rest.domain.pullrequest.ProjectKey;
import com.cdancy.bitbucket.rest.domain.pullrequest.PullRequest;
import com.cdancy.bitbucket.rest.options.CreatePullRequest;

@Component
@Profile("bitbucket")
public class BitBucketPullRequestService implements PullRequestService {

    private static Logger log = LoggerFactory.getLogger(BitBucketPullRequestService.class);

    @Override
    public void pr(Repository localRepository, GitSettings settings, String title, String body) {
        if (settings.pushToRemoteEnabled() && settings.pullRequestEnabled()) {
            try {
                String bitbucketUrl = settings.uri();
                String username = settings.username();
                String password = settings.password();

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
                    "refs/heads/" + settings.base(),
                    repository,
                    null,  // state
                    false,  // not a tag
                    settings.base(),  // displayId
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
                    log.info("Pull request created: {}", pullRequest.links().self());
                } else {
                    log.error("Failed to create pull request");
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to create pull request", e);
            }
        } else {
            log.info("Pull request not enabled!");
        }
    }

    private String[] determineProjectInfo(String remoteUrl) {
        String[] urlParts = remoteUrl.split("/");
        return new String[]{urlParts[urlParts.length - 2], urlParts[urlParts.length - 1].replace(".git", "")};
    }
}
