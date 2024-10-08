package org.cftoolsuite.domain;

public record FileSource (
    String filePath,
    String content) {

    public FileSource(
        String filePath,
        String content) {
        this.filePath = filePath;
        this.content = content;
    }

}
