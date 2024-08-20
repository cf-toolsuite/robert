package org.cftoolsuite.util;

import org.eclipse.jgit.lib.Repository;

public interface PullRequestService {
    void pr(Repository repo, GitSettings settings, String title, String body);
}
