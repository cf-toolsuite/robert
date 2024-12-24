package org.cftoolsuite.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RefactoringService extends Function<GitRequest, GitResponse>{

    Logger log = LoggerFactory.getLogger(RefactoringService.class);

    default GitResponse completeRefactor(GitClient gitClient, PullRequestClientFactory pullRequestClientFactory, GitRequest request, Repository repo, Map<String, String> targetMap, String prompt) {
        var branchName = String.join("-","refactor", UUID.randomUUID().toString());
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var commitMessage = String.format("Refactored by %s on %s", this.getClass().getName(), LocalDateTime.now().format(formatter));
        commitToNewBranch(gitClient, repo, targetMap, branchName, commitMessage);
        pushNewBranchToRemote(gitClient, request, repo, branchName);
        var pullRequestUrl = createPullRequest(pullRequestClientFactory, request, repo, branchName, commitMessage);
        log.info("Refactoring completed on {}.", branchName);
        return new GitResponse(prompt, request.uri(), branchName, pullRequestUrl, targetMap.keySet());
    }

    default void commitToNewBranch(GitClient gitClient, Repository repo, Map<String, String> targetMap, String branchName, String commitMessage) {
        gitClient.writeFiles(repo, targetMap, branchName, commitMessage);
    }

    default void pushNewBranchToRemote(GitClient gitClient, GitRequest request, Repository repo, String branchName) {
        gitClient.push(request, repo, branchName);
    }

    default String createPullRequest(PullRequestClientFactory pullRequestClientFactory, GitRequest request, Repository repo, String branchName, String commitMessage) {
        return pullRequestClientFactory.get(request.uri()).pr(repo, request, branchName, commitMessage);
    }
}
