package org.cftoolsuite.domain;

import org.apache.commons.lang3.StringUtils;

public record IngestRequest(
        String uri,
        String username,
        String password,
        String commit) {

    public IngestRequest(
            String uri,
            String username,
            String password,
            String commit) {
        this.uri = (uri != null) ? uri : "";
        this.username = username;
        this.password = (password != null) ? password : "";
        this.commit = (commit != null) ? commit : "";
    }

    public boolean isAuthenticated() {
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);
    }

    public static class IngestRequestBuilder {
        private String uri;
        private String username;
        private String password;
        private String commit;

        public IngestRequestBuilder uri(String uri) {
            this.uri = (uri != null) ? uri : "";
            return this;
        }

        public IngestRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public IngestRequestBuilder password(String password) {
            this.password = (password != null) ? password : "";
            return this;
        }

        public IngestRequestBuilder commit(String commit) {
            this.commit = (commit != null) ? commit : "";
            return this;
        }

        public IngestRequest build() {
            return new IngestRequest(uri, username, password, commit);
        }
    }

    public static IngestRequestBuilder builder() {
        return new IngestRequestBuilder();
    }
}
