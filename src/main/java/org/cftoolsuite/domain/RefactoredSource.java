package org.cftoolsuite.domain;

public record RefactoredSource (
    String filePath,
    String content) {

    public RefactoredSource(
        String filePath,
        String content) {
        this.filePath = filePath;
        this.content = content;
    }

}
