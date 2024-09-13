package org.cftoolsuite.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.cftoolsuite.domain.GitRequest;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitClientTest {

    private GitClient gitClient;
    private GitRequest validRequest;
    private GitRequest invalidRequest;

    private static final String VALID_URI = "https://github.com/cf-toolsuite/robert.git";
    private static final String INVALID_URI = "http://github.com/cf-toolsuite/robert";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitClient = new GitClient();
        validRequest = new GitRequest(VALID_URI, "main", null, null, null, Set.of(File.separator), null, false, false);
        invalidRequest = new GitRequest(INVALID_URI, "main", null, null, null, Set.of(File.separator), null, false, false);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up temporary files
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception e) {
                }
            });
    }

    @Test
    void testGetRepository_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        assertThat(repo).isNotNull();
        assertThat(repo.getDirectory()).exists();
        assertThat(repo.getWorkTree()).exists();
    }

    @Test
    void testGetRepository_InvalidUri() {
        assertThatThrownBy(() -> gitClient.getRepository(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URI scheme must be https");
    }

    @Test
    void testGetOrigin_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        String origin = gitClient.getOrigin(repo);
        assertThat(origin).isEqualTo(VALID_URI);
    }

    @Test
    void testGetLatestCommit_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        RevCommit latestCommit = gitClient.getLatestCommit(repo);
        assertThat(latestCommit).isNotNull();
        assertThat(latestCommit.getFullMessage()).isNotEmpty();
    }

    @Test
    void testReadFile_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        RevCommit latestCommit = gitClient.getLatestCommit(repo);
        Map<String, String> fileContent = gitClient.readFile(repo, "README.md", latestCommit.getName());
        assertThat(fileContent).isNotEmpty();
        assertThat(fileContent).containsKey("README.md");
        assertThat(fileContent.get("README.md")).contains("robert");
    }

    @Test
    void testReadFile_FileNotFound() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        RevCommit latestCommit = gitClient.getLatestCommit(repo);
        Map<String, String> fileContent = gitClient.readFile(repo, "NonExistentFile.txt", latestCommit.getName());
        assertThat(fileContent).isEmpty();
    }

    @Test
    void testReadFiles_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        Set<String> paths = Set.of("README.md", "build.gradle");
        Set<String> allowedExtensions = Set.of("md", "gradle");
        Map<String, String> files = gitClient.readFiles(repo, paths, allowedExtensions, null);
        assertThat(files).hasSize(2);
        assertThat(files).containsKeys("README.md", "build.gradle");
    }

    @Test
    void testWriteFile_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        String filePath = "test-file.txt";
        String contents = "This is a test file.";
        String branch = "test-branch";

        gitClient.writeFile(repo, filePath, contents, branch);

        File writtenFile = new File(repo.getWorkTree(), filePath);
        assertThat(writtenFile).exists();
        assertThat(Files.readString(writtenFile.toPath())).isEqualTo(contents);
    }

    @Test
    void testWriteFiles_HappyPath() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        Map<String, String> sourceMap = Map.of(
            "test-file1.txt", "Content of file 1",
            "test-file2.txt", "Content of file 2"
        );
        String branch = "test-branch";
        String commitMessage = "Test commit";

        gitClient.writeFiles(repo, sourceMap, branch, commitMessage);

        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            File writtenFile = new File(repo.getWorkTree(), entry.getKey());
            assertThat(writtenFile).exists();
            assertThat(Files.readString(writtenFile.toPath())).isEqualTo(entry.getValue());
        }
    }

    @Test
    void testPush_DisabledPush() throws Exception {
        Repository repo = gitClient.getRepository(validRequest);
        gitClient.push(validRequest, repo, "main");
        // Since push is disabled, we can't verify the remote state.
        // We're just ensuring no exception is thrown.
    }

}