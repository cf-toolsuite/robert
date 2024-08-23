package org.cftoolsuite.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.azd.connection.Connection;
import org.azd.exceptions.AzDException;
import org.azd.git.GitApi;
import org.azd.git.types.GitPullRequest;
import org.azd.git.types.GitRepository;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("azure-devops")
public class AzureDevopsPullRequestService implements PullRequestService {

    private static Logger log = LoggerFactory.getLogger(AzureDevopsPullRequestService.class);

    @Override
    public void pr(Repository localRepository, GitSettings settings, String title, String body) {
        if (settings.pushToRemoteEnabled() && settings.pullRequestEnabled()) {
            try {
                String organizationUrl = extractOrganizationUrl(settings.uri());
                Connection connection = new Connection(organizationUrl, settings.password());

                GitApi gitApi = new GitApi(connection);

                String[] projectInfo = determineProjectInfo(getRemoteUrl(localRepository));
                String repoName = projectInfo[1];

                // Get the repository
                GitRepository repo = gitApi.getRepository(repoName);

                // Create the pull request
                String sourceRefName = "refs/heads/" + localRepository.getBranch();
                String targetRefName = "refs/heads/" + settings.base();

                GitPullRequest createdPR = gitApi.createPullRequest(
                    repo.getId(),
                    sourceRefName,
                    targetRefName,
                    title,
                    body,
                    null  // No reviewers specified
                );

                log.info("Pull request created: {}", createdPR.getUrl());
            } catch (AzDException | IOException | URISyntaxException e) {
                throw new RuntimeException("Failed to create pull request", e);
            }
        } else {
            log.info("Pull request not enabled!");
        }
    }

    private String extractOrganizationUrl(String uri) throws URISyntaxException {
        URI fullUri = new URI(uri);
        String host = fullUri.getHost();
        String[] parts = host.split("\\.");
        if (parts.length >= 2 && "visualstudio".equals(parts[parts.length - 2])) {
            return "https://" + parts[0] + ".visualstudio.com";
        } else {
            return "https://dev.azure.com/" + parts[0];
        }
    }

    private String[] determineProjectInfo(String remoteUrl) {
        String[] urlParts = remoteUrl.split("/");
        return new String[]{urlParts[urlParts.length - 2], urlParts[urlParts.length - 1].replace(".git", "")};
    }

}
