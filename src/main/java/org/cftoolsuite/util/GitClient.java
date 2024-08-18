package org.cftoolsuite.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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


@Component
public class GitClient {

    private static Logger log = LoggerFactory.getLogger(GitClient.class);

    public Repository getRepository(GitSettings settings) {
        Repository result = null;
        String uri = settings.uri();
        Assert.hasText(uri, "URI of remote Git repository must be specified");
        Assert.isTrue(uri.startsWith("https://"), "URI scheme must be https");
        Assert.isTrue(uri.endsWith(".git"), "URI must end with .git");
        String path = String.join(File.separator, "tmp", uri.substring(uri.lastIndexOf("/") + 1).replace(".git",""));
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
            if (settings.isAuthenticated()) {
                String username = settings.username();
                String password = settings.password();
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
            log.warn(String.format("Cannot clone Git repository at %s", uri), e);
        }
        return result;
    }

    // @see https://stackoverflow.com/questions/42820282/get-the-latest-commit-in-a-repository-with-jgit
    public RevCommit getLatestCommit(Repository repo) throws IOException, GitAPIException {
        RevCommit latestCommit = null;
        try (
            Git git = new Git(repo);
            RevWalk walk = new RevWalk(repo);
        ) {
            List<Ref> branches = git.branchList().call();
            latestCommit = branches
                .stream()
                .map(branch -> {
                    try {
                        return walk.parseCommit(branch.getObjectId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted(Comparator.comparing((RevCommit commit) -> commit.getAuthorIdent().getWhen()).reversed())
                .findFirst()
                .orElse(null);

            if (latestCommit != null) {
                log.info("Latest commit with id {} was made {} on {} by {}", latestCommit.getId().name(), latestCommit.getAuthorIdent().getWhen(),
                        latestCommit.getShortMessage(), latestCommit.getAuthorIdent().getName());
            }
        }
        return latestCommit;
    }

    public Map<String, String> readFile(Repository repo, String filePath, String commit) throws IOException {
        Map<String, String> result = new HashMap<>();
        ObjectId oid = repo.resolve(commit);
        RevCommit revision = repo.parseCommit(oid);
        try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, revision.getTree())) {
            if (treeWalk != null) {
                byte[] bytes = repo.open(treeWalk.getObjectId(0)).getBytes();
                String path = treeWalk.getPathString();
                String contents = new String(bytes, StandardCharsets.UTF_8);
                result.put(path, contents);
                return result;
            } else {
                throw new IllegalArgumentException(String.format("No file found for commitId=%s and filePath=%s", commit, filePath));
            }
        }
    }

    public Map<String,String> readFiles(Repository repo, Set<String> paths, String... commits) throws IOException, GitAPIException {
        Map<String, String> result = new HashMap<>();
        String commitToUse = (commits.length > 0) ? commits[0] : getLatestCommit(repo).name();
        for (String path: paths) {
            if (!path.contains("/") && path.contains(".")) {
                result.putAll(readFilesFromPackages(repo, Set.of(path), commitToUse));
            } else if (path.contains("/")) {
                result.putAll(readFile(repo, path, commitToUse));
            } else {
                log.warn("{} is not a valid path!  Skipping.", path);
            }
        }
        return result;
    }

    public Map<String, String> readFilesFromPackages(Repository repo, Set<String> packageNames, String... commits) throws IOException, GitAPIException {
        Map<String, String> result = new HashMap<>();
        // Determine the commit to use
        String commitToUse = (commits.length > 0) ? commits[0] : getLatestCommit(repo).name();
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

    public void writeFile(Repository repo, String filePath, String contents, String branch) throws IOException, GitAPIException {
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
            throw new IOException("Failed to write file to branch: " + branch, e);
        }
    }

    public void writeFiles(Repository repo, Map<String, String> sourceMap, String branch, String commitMessage) throws IOException, GitAPIException {
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
        } catch (IOException | GitAPIException e) {
            throw new IOException("Failed to write files and commit to branch: " + branch, e);
        }
    }

    public void push(GitSettings settings, Repository repo, String branch) {
        try (Git git = new Git(repo)) {
            if (settings.pushToRemoteEnabled() && settings.isAuthenticated()) {
                CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(settings.username(), settings.password());
                git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(credentialsProvider)
                    .add(String.format("refs/heads/%s", branch))
                    .call();
                log.info("Pushed changes to remote [{}].", branch);
            } else {
                log.info("Push to remote not enabled!");
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(String.format("Failed to push to remote [%s]", branch), e);
        }
    }
}

