package org.cftoolsuite.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GithubPullRequestService implements PullRequestService {

    private static Logger log = LoggerFactory.getLogger(GithubPullRequestService.class);

    @Override
    public void pr(Repository localRepository, GitSettings settings, String title, String body) {
        if (settings.pullRequestEnabled()) {
            try {
                GitHub github = new GitHubBuilder().withPassword(settings.username(), settings.password()).build();
                GHRepository repository = determineGithubRepository(github, getRemoteUrl(localRepository));
                GHPullRequest pullRequest = repository.createPullRequest(
                    title,
                    localRepository.getBranch(),
                    title,
                    body
                );
                log.info("Pull request created: {}", pullRequest.getHtmlUrl());
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to create pull request", e);
            }
        } else {
            log.info("Pull request not enabled!");
        }
    }

    private GHRepository determineGithubRepository(GitHub github, String remoteUrl) throws IOException {
        String[] urlParts = remoteUrl.split("/");
        String owner = urlParts[urlParts.length - 2];
        String repoName = urlParts[urlParts.length - 1].replace(".git", "");
        GHRepository result = github.getRepository(owner + "/" + repoName);
        log.info("GitHub repository is {}", result.getFullName());
        return result;
    }

    private String getRemoteUrl(Repository repository) throws URISyntaxException {
        String remoteUrl = null;
        List<RemoteConfig> remotes = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
        for (RemoteConfig remote : remotes) {
            if ("origin".equals(remote.getName())) {
                List<URIish> uris = remote.getURIs();
                if (!uris.isEmpty()) {
                    remoteUrl = uris.get(0).toString();
                    break;
                }
            }
        }
        if (remoteUrl == null) {
            throw new IllegalStateException("No 'origin' remote found.");
        }
        return remoteUrl;
    }

}
