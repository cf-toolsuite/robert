package org.cftoolsuite.domain;

public record LanguageExtensions(String language, String extensions) {
    @Override
    public String toString() {
        return language;
    }
}
