package org.cftoolsuite.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.cftoolsuite.domain.GitRequest;
import org.eclipse.jgit.lib.Repository;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitlabPullRequestClient implements PullRequestClient {

    private static Logger log = LoggerFactory.getLogger(GitlabPullRequestClient.class);

    @Override
    public String pr(Repository localRepository, GitRequest request, String title, String body) {
        String result = null;
        if (request.pushToRemoteEnabled() && request.pullRequestEnabled()) {
            try {
                GitLabApi gitLabApi = GitLabApi.oauth2Login(request.uri(), request.username(), request.password());
                String projectId = determineGitlabProjectId(getRemoteUrl(localRepository));
                MergeRequest mergeRequest = gitLabApi.getMergeRequestApi().createMergeRequest(
                    projectId,
                    localRepository.getBranch(),
                    request.base(),
                    title,
                    body,
                    getAssigneeIdFromUsername(gitLabApi, request.username())
                );
                result = mergeRequest.getWebUrl().toString();
                log.info("Merge request created: {}", result);
            } catch (GitLabApiException | URISyntaxException | IOException e) {
                throw new RuntimeException("Failed to create merge request", e);
            }
        } else {
            log.info("Merge request not enabled!");
        }
        return result;
    }

    private String determineGitlabProjectId(String remoteUrl) {
        String[] urlParts = remoteUrl.split("/");
        return urlParts[urlParts.length - 2] + "/" + urlParts[urlParts.length - 1].replace(".git", "");
    }

    private Long getAssigneeIdFromUsername(GitLabApi gitLabApi, String username) {
        try {
            UserApi userApi = gitLabApi.getUserApi();
            List<User> users = userApi.findUsers(username);
            if (!users.isEmpty()) {
                User user = users.get(0);
                return user.getId();
            } else {
                throw new RuntimeException("User not found: " + username);
            }
        } catch (GitLabApiException e) {
            throw new RuntimeException("Error fetching user information", e);
        }
    }

    @Override
    public String uriPrefix() {
        return "https://gitlab.com/";
    }

}
