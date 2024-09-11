package org.cftoolsuite.client;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.domain.GitRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Component
public class GitClient {

    private static Logger log = LoggerFactory.getLogger(GitClient.class);

    public Repository getRepository(GitRequest request) {
        Repository result = null;
        String uri = request.uri();
        Assert.hasText(uri, "URI of remote Git repository must be specified");
        Assert.isTrue(uri.startsWith("https://"), "URI scheme must be https");
        Assert.isTrue(uri.endsWith(".git"), "URI must end with .git");
        String path = String.join(File.separator, "tmp", uri.substring(uri.lastIndexOf("/") + 1).replace(".git", ""));
        try {
            File directory = new File(path);
            Path p = Paths.get(directory.toURI());
            if (Files.exists(p)) {
                Files
                    .walk(p)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            Files.deleteIfExists(p);
            if (request.isAuthenticated()) {
                String username = request.username();
                String password = request.password();
                Git
                    .cloneRepository()
                    .setURI(uri)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                    .setDirectory(directory)
                    .setCloneAllBranches(true)
                    .call()
                    .close();
            } else {
                Git
                    .cloneRepository()
                    .setURI(uri)
                    .setDirectory(directory)
                    .setCloneAllBranches(true)
                    .call()
                    .close();
            }
            result = Git.open(directory).getRepository();
            log.info("Cloned {}", result.toString());
        } catch (GitAPIException | IOException e) {
            throw new GitOperationException(String.format("Could not clone Git repository at %s", uri), e);
        }
        return result;
    }

    public String getOrigin(Repository repo) throws IOException {
        String result = null;
        try (Git git = new Git(repo)) {
            result = git.getRepository().getConfig().getString("remote", "origin", "url");
        }
        return result;
    }

    // @see
    // https://stackoverflow.com/questions/42820282/get-the-latest-commit-in-a-repository-with-jgit
    public RevCommit getLatestCommit(Repository repo) {
        Assert.notNull(repo, "Repository must not be null");
        RevCommit latestCommit = null;
        try (
            Git git = new Git(repo);
            RevWalk walk = new RevWalk(repo);) {
            List<Ref> branches = git.branchList().call();
            latestCommit = branches
                    .stream()
                    .map(branch -> {
                        try {
                            return walk.parseCommit(branch.getObjectId());
                        } catch (IOException e) {
                            throw new GitOperationException("Trouble determining latest commit", e);
                        }
                    })
                    .sorted(Comparator.comparing((RevCommit commit) -> commit.getAuthorIdent().getWhen()).reversed())
                    .findFirst()
                    .orElse(null);

            if (latestCommit != null) {
                log.info("Latest commit with id {} was made {} on {} by {}",
                    latestCommit.getId().name(),
                    latestCommit.getAuthorIdent().getWhen(),
                    latestCommit.getShortMessage(),
                    latestCommit.getAuthorIdent().getName()
                );
            }
        } catch (GitAPIException e) {
            throw new GitOperationException("Trouble obtaining list of branches", e);
        }
        return latestCommit;
    }

    public Map<String, String> readFile(Repository repo, String filePath, String commit) throws IOException {
        Assert.notNull(repo, "Repository must not be null");
        Assert.hasText(filePath, "File path must be specified");
        Assert.hasText(commit, "Commit ID must be specified");
        Map<String, String> result = new HashMap<>();
        ObjectId oid = repo.resolve(commit);
        RevCommit revision = repo.parseCommit(oid);
        try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, revision.getTree())) {
            if (treeWalk != null) {
                byte[] bytes = repo.open(treeWalk.getObjectId(0)).getBytes();
                String path = treeWalk.getPathString();
                log.info("-- Obtaining contents of {}", path);
                String contents = new String(bytes, StandardCharsets.UTF_8);
                result.put(path, contents);
                return result;
            } else {
                throw new IllegalArgumentException(
                        String.format("No file found for commitId=%s and filePath=%s", commit, filePath));
            }
        }
    }

    public Map<String, String> readFiles(Repository repo, Set<String> paths, Set<String> allowedExtensions,
            String commit) throws IOException {
        Assert.notNull(repo, "Repository must not be null");
        String commitToUse = StringUtils.defaultIfBlank(commit, getLatestCommit(repo).name());
        ObjectId commitId = repo.resolve(commitToUse);
        RevCommit revision = repo.parseCommit(commitId);

        if (CollectionUtils.isEmpty(paths)) {
            return readAllFiles(repo, revision, allowedExtensions, commitToUse);
        } else {
            return
                paths
                    .stream()
                    .filter(path -> isValidPath(path))
                    .flatMap(path -> readPathContent(repo, path, allowedExtensions, commitToUse).entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        }
    }

    private Map<String, String> readAllFiles(Repository repo, RevCommit revision, Set<String> allowedExtensions,
            String commitToUse) throws IOException {
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(revision.getTree());
            treeWalk.setRecursive(true);
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    try {
                        return treeWalk.next();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public String next() {
                    return treeWalk.getPathString();
                }
            }, Spliterator.ORDERED), false)
                .filter(path -> isAllowedExtension(path, allowedExtensions))
                .flatMap(path -> {
                    try {
                        return readFile(repo, path, commitToUse).entrySet().stream();
                    } catch (IOException e) {
                        log.warn("Trouble reading file '{}': {}", path, e.getMessage());
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        }
    }

    private boolean isValidPath(String path) {
        return
            Optional
                .of(path)
                .filter(p -> Stream.of(
                    p.contains(".") && !p.contains("/"),
                    p.contains("/")
                ).anyMatch(Boolean::booleanValue))
                .map(p -> true)
                .orElseGet(() -> {
                    log.warn("{} is not a valid path! Skipping.", path);
                    return false;
                });
    }

    private Map<String, String> readPathContent(Repository repo, String path, Set<String> allowedExtensions,
            String commitToUse) {
        if (isAllowedExtension(path, allowedExtensions)) {
            try {
                return
                    isJavaPackage(path)
                        ? readFilesFromPackages(repo, Set.of(path), commitToUse)
                        : readFile(repo, path, commitToUse);
            } catch (IOException e) {
                log.warn("Trouble reading path '{}': {}", path, e.getMessage());
                return Collections.emptyMap();
            }
        } else {
            log.warn("File '{}' skipped due to disallowed extension.", path);
            return Collections.emptyMap();
        }
    }

    private boolean isJavaPackage(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.chars()
                  .filter(ch -> ch == '.')
                  .count() > 1;
    }

    private boolean isAllowedExtension(String path, Set<String> allowedExtensions) {
        if (CollectionUtils.isEmpty(allowedExtensions)) {
            return true;
        }
        return Optional.of(path)
            .filter(p -> p.lastIndexOf('.') != -1)
            .map(p -> p.substring(p.lastIndexOf('.') + 1))
            .map(allowedExtensions::contains)
            .orElse(false);
    }

    public Map<String, String> readFilesFromPackages(Repository repo, Set<String> packageNames, String commit)
            throws IOException {
        Assert.notNull(repo, "Repository must not be null");
        Map<String, String> result = new HashMap<>();
        // Determine the commit to use
        String commitToUse = StringUtils.isNotBlank(commit) ? commit : getLatestCommit(repo).name();
        // Resolve the commit object
        ObjectId commitId = repo.resolve(commitToUse);
        RevCommit revision = repo.parseCommit(commitId);
        // Iterate over each package name
        for (String packageName : packageNames) {
            // Convert package name to file path
            String packagePath = "src/main/java/" + packageName.replace('.', '/');
            // Use TreeWalk to find all .java files in the package
            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(revision.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String path = treeWalk.getPathString();
                    // Check if the file is in the desired package and is a .java file
                    if (path.startsWith(packagePath) && path.endsWith(".java")) {
                        log.info("-- Obtaining contents of {}", path);
                        // Read the file contents
                        byte[] bytes = repo.open(treeWalk.getObjectId(0)).getBytes();
                        String fileContent = new String(bytes, StandardCharsets.UTF_8);
                        // Store the file path and content in the map
                        result.put(path, fileContent);
                    }
                }
            }
        }
        return result;
    }

    public void writeFile(Repository repo, String filePath, String contents, String branch) {
        Assert.notNull(repo, "Repository must not be null");
        Assert.hasText(filePath, "File path must be specified");
        Assert.hasText(contents, "File contents must be specified");
        Assert.hasText(branch, "Git branch name must be specified");
        try (Git git = new Git(repo)) {
            // Check if the branch exists
            boolean branchExists = repo.findRef(branch) != null;
            if (!branchExists) {
                // Create the branch if it doesn't exist
                git.branchCreate().setName(branch).call();
            }
            // Checkout the branch
            git.checkout().setName(branch).call();
            // Write the contents to the file
            Path path = Paths.get(repo.getWorkTree().getAbsolutePath(), filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
            // Add the file to the index
            git.add().addFilepattern(filePath).call();
            // Removed the commit call from here
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to write file to branch: " + branch, e);
        }
    }

    public void writeFiles(Repository repo, Map<String, String> sourceMap, String branch, String commitMessage) {
        try (Git git = new Git(repo)) {
            // Iterate over the sourceMap and write each file
            for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                String filePath = entry.getKey();
                String contents = entry.getValue();
                writeFile(repo, filePath, contents, branch);
            }
            // Commit all changes with the provided commit message
            if (StringUtils.isNotBlank(commitMessage)) {
                git.commit().setMessage(commitMessage).call();
            }
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to write files and commit to branch: " + branch, e);
        }
    }

    public void push(GitRequest request, Repository repo, String branch) {
        Assert.notNull(request, "Git request must not be null");
        Assert.notNull(repo, "Repository must not be null");
        try (Git git = new Git(repo)) {
            if (request.pushToRemoteEnabled()) {
                if (request.isAuthenticated()) {
                    CredentialsProvider credentialsProvider =
                        new UsernamePasswordCredentialsProvider(
                            request.username(),
                            request.password()
                        );
                    git
                        .push()
                        .setRemote("origin")
                        .setCredentialsProvider(credentialsProvider)
                        .add(String.format("refs/heads/%s", branch))
                        .call();
                    log.info("Pushed changes to remote [{}].", branch);
                } else {
                    log.warn("Push to remote requires authentication! Skipping push.");
                }
            } else {
                log.info("Push to remote not enabled!");
            }
        } catch (GitAPIException e) {
            throw new GitOperationException(String.format("Failed to push to remote [%s]", branch), e);
        }
    }
}
