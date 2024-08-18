package org.cftoolsuite.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public record GitSettings(
        String uri,
        String username,
        String password,
        String commit,
        Set<String> filePaths,
        boolean pushToRemoteEnabled) {

    public GitSettings(
            String uri,
            String username,
            String password,
            String commit,
            Set<String> filePaths,
            boolean pushToRemoteEnabled) {
        this.uri = (uri != null) ? uri : "";
        this.username = username;
        this.password = (password != null) ? password : "";
        this.commit = commit;
        this.filePaths = (filePaths != null) ? filePaths : new HashSet<>();
        this.pushToRemoteEnabled = pushToRemoteEnabled;
    }

    public boolean isAuthenticated() {
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);
    }

    public static class GitSettingsBuilder {
        private String uri = "";
        private String username;
        private String password = "";
        private String commit;
        private Set<String> filePaths = new HashSet<>();
        private boolean pushToRemoteEnabled;

        public GitSettingsBuilder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public GitSettingsBuilder username(String username) {
            this.username = username;
            return this;
        }

        public GitSettingsBuilder password(String password) {
            this.password = password;
            return this;
        }

        public GitSettingsBuilder commit(String commit) {
            this.commit = commit;
            return this;
        }

        public GitSettingsBuilder filePaths(Set<String> filePaths) {
            this.filePaths = filePaths;
            return this;
        }

        public GitSettingsBuilder pushToRemoteEnabled(boolean pushToRemoteEnabled) {
            this.pushToRemoteEnabled = pushToRemoteEnabled;
            return this;
        }

        public GitSettings build() {
            return new GitSettings(uri, username, password, commit, filePaths, pushToRemoteEnabled);
        }
    }

    public static GitSettingsBuilder builder() {
        return new GitSettingsBuilder();
    }
}
