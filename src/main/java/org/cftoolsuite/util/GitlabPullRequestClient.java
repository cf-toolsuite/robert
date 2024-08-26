package org.cftoolsuite.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

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
    public void pr(Repository localRepository, GitSettings settings, String title, String body) {
        if (settings.pushToRemoteEnabled() && settings.pullRequestEnabled()) {
            try {
                GitLabApi gitLabApi = GitLabApi.oauth2Login(settings.uri(), settings.username(), settings.password());
                String projectId = determineGitlabProjectId(getRemoteUrl(localRepository));
                MergeRequest mergeRequest = gitLabApi.getMergeRequestApi().createMergeRequest(
                    projectId,
                    localRepository.getBranch(),
                    settings.base(),
                    title,
                    body,
                    getAssigneeIdFromUsername(gitLabApi, settings.username())
                );
                log.info("Merge request created: {}", mergeRequest.getWebUrl());
            } catch (GitLabApiException | URISyntaxException | IOException e) {
                throw new RuntimeException("Failed to create merge request", e);
            }
        } else {
            log.info("Merge request not enabled!");
        }
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
