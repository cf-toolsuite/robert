package org.cftoolsuite.domain;

import java.util.HashSet;
import java.util.Set;

public record GitResponse(
        String prompt,
        String uri,
        String branch,
        String pullRequestUrl,
        Set<String> impactedFileSet
) {

    public GitResponse(
            String prompt,
            String uri,
            String branch,
            String pullRequestUrl,
            Set<String> impactedFileSet
        ) {
        this.prompt = prompt;
        this.uri = uri;
        this.branch = branch;
        this.pullRequestUrl = pullRequestUrl;
        this.impactedFileSet = (impactedFileSet != null) ? impactedFileSet : new HashSet<>();
    }

    public static GitResponse noneFor(String uri) {
        return new GitResponse(null, uri, null, null, null);
    }

}
