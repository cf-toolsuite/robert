package org.cftoolsuite.util;

import java.util.HashSet;
import java.util.Set;

public record GitResponse(
        String uri,
        String branch,
        String pullRequestUrl,
        Set<String> impactedFileSet
) {

    public GitResponse(
            String uri,
            String branch,
            String pullRequestUrl,
            Set<String> impactedFileSet
        ) {
        this.uri = uri;
        this.branch = branch;
        this.pullRequestUrl = pullRequestUrl;
        this.impactedFileSet = (impactedFileSet != null) ? impactedFileSet : new HashSet<>();
    }

    public static GitResponse noneFor(String uri) {
        return new GitResponse(uri, null, null, null);
    }

}
