package org.cftoolsuite.domain;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public record IngestRequest(
        String uri,
        String username,
        String password,
        String commit,
        Set<String> allowedExtensions) {

    public IngestRequest(
            String uri,
            String username,
            String password,
            String commit,
            Set<String> allowedExtensions) {
        this.uri = (uri != null) ? uri : "";
        this.username = username;
        this.password = (password != null) ? password : "";
        this.commit = (commit != null) ? commit : "";
        this.allowedExtensions = (allowedExtensions != null) ? allowedExtensions : new HashSet<>();
    }

    public boolean isAuthenticated() {
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);
    }

    public static class IngestRequestBuilder {
        private String uri;
        private String username;
        private String password;
        private String commit;
        private Set<String> allowedExtensions;

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

        public IngestRequestBuilder allowedExtensions(Set<String> allowedExtensions) {
            this.allowedExtensions = (allowedExtensions != null) ? allowedExtensions : new HashSet<>();
            return this;
        }

        public IngestRequest build() {
            return new IngestRequest(uri, username, password, commit, allowedExtensions);
        }
    }

    public static IngestRequestBuilder builder() {
        return new IngestRequestBuilder();
    }
}
