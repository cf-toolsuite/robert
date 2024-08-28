package org.cftoolsuite.client;

import java.io.IOException;
import java.net.URISyntaxException;

import org.cftoolsuite.domain.GitRequest;
import org.eclipse.jgit.lib.Repository;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GithubPullRequestClient implements PullRequestClient {

    private static Logger log = LoggerFactory.getLogger(GithubPullRequestClient.class);

    @Override
    public String pr(Repository localRepository, GitRequest request, String title, String body) {
        String result = null;
        if (request.pushToRemoteEnabled() && request.pullRequestEnabled()) {
            try {
                GitHub github = new GitHubBuilder().withPassword(request.username(), request.password()).build();
                GHRepository repository = determineGithubRepository(github, getRemoteUrl(localRepository));
                GHPullRequest pullRequest = repository.createPullRequest(
                    title,
                    localRepository.getBranch(),
                    request.base(),
                    body
                );
                result = pullRequest.getHtmlUrl().toString();
                log.info("Pull request created: {}", result);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to create pull request", e);
            }
        } else {
            log.info("Pull request not enabled!");
        }
        return result;
    }

    private GHRepository determineGithubRepository(GitHub github, String remoteUrl) throws IOException {
        String[] urlParts = remoteUrl.split("/");
        String owner = urlParts[urlParts.length - 2];
        String repoName = urlParts[urlParts.length - 1].replace(".git", "");
        GHRepository result = github.getRepository(owner + "/" + repoName);
        log.info("GitHub repository is {}", result.getFullName());
        return result;
    }

    @Override
    public String uriPrefix() {
        return "https://github.com/";
    }

}
