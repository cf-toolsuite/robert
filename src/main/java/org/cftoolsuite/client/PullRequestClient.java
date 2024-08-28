package org.cftoolsuite.client;

import java.net.URISyntaxException;
import java.util.List;

import org.cftoolsuite.domain.GitRequest;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public interface PullRequestClient {

    String pr(Repository repo, GitRequest request, String title, String body);

    String uriPrefix();

    default String getRemoteUrl(Repository repository) throws URISyntaxException {
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
