# R*bert

* [Endpoints](#endpoints)
  * [Clone](#clone)
  * [Ingest](#ingest)
  * [Refactor](#refactor)

## Endpoints

Endpoints below work with [GitRequest.java](../src/main/java/org/cftoolsuite/domain/GitRequest.java).

The minimum required inputs are:

* `uri` - a remote Git repository, must be accessible via `https` protocol
* `filePaths` - either a set of relative file paths from the root of the repository or a set of package names

Optional inputs are:

* `base` - the base branch of your repository where changes would eventually be pulled into; defaults to `main`
* `username` - your Git repository provider account username
* `password` - your Git repository provider account password
  * if you have a Github account, then this value should be set to a classic [personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic) with full `repo` permissions.
* `commit` - a commit hash, if not supplied the latest commit on origin/main is used
* `pushToRemoteEnabled` - whether or not to git push updates on your local branch to remote; if you've set this value to `true` then you must also supply `username` and `password` values as push operation is authenticated
* `pullRequestEnabled` - whether or not to file a pull request; if you've set this value to `true` then you must also supply `username` and `password` values as pull request operation is authenticated

Note: if you're working with a private repository you will be required to supply `username` and `password` values, as clone and push operations will be authenticated

### Clone

```python
POST /clone
```

Clones source from a Git repository to your local desktop

### Ingest

This endpoint is only available when `spring.profiles.active` includes `advanced` mode.  You must ingest a repository before initiating a refactor request.

```python
POST /ingest
```

Clones then ingests all files from a Git repository.  Contents of each file along with appropriate metadata are stored in a Vector database.


### Refactor

```python
POST /refactor
```

Clones and refactors source

> R*bert clones the remote repository, iterates over a set of file paths, and applies updates to each file based upon criteria in your prompt.  It writes updates to a local branch, and if configured to do so, it will push those updates back to origin.  Note: Refactoring does not take into account dependencies or relationships within the set of file paths.

#### Sample interaction

```bash
❯ http POST :8080/refactor uri=https://github.com/cf-toolsuite/cf-butler.git filePaths:='["org.cftoolsuite.cfapp.domain.accounting.application"]'

HTTP/1.1 200
Connection: keep-alive
Content-Type: application/json
Date: Thu, 29 Aug 2024 18:43:02 GMT
Keep-Alive: timeout=60
Transfer-Encoding: chunked

{
    "branch": "refactor-a3b21c59-a5c2-4d62-a720-097a328dc894",
    "impactedFileSet": [
        "src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageMonthly.java",
        "src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageYearly.java",
        "src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageReport.java"
    ],
    "pullRequestUrl": null,
    "uri": "https://github.com/cf-toolsuite/cf-butler.git"
}
```

#### Sample log output

```bash
11:40:01.323 [main] INFO  org.cftoolsuite.RobertApplication - Started RobertApplication in 2.62 seconds (process running for 2.888)
11:41:15.196 [tomcat-handler-0] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring DispatcherServlet 'dispatcherServlet'
11:41:15.196 [tomcat-handler-0] INFO  o.s.web.servlet.DispatcherServlet - Initializing Servlet 'dispatcherServlet'
11:41:15.198 [tomcat-handler-0] INFO  o.s.web.servlet.DispatcherServlet - Completed initialization in 2 ms
11:42:51.285 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - Cloned Repository[/home/cphillipson/Documents/development/pivotal/cf/robert/tmp/cf-butler/.git]
11:42:51.287 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - Latest commit with id 6562b05bf171d482c303895ad5206e1fc7962cae was made Tue Aug 27 08:02:12 PDT 2024 on Upgrade bcprov-jdk18on to 1.78.1 by Chris Phillipson
11:42:51.289 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageMonthly.java
11:42:51.289 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageReport.java
11:42:51.289 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageYearly.java
11:42:51.289 [tomcat-handler-2] INFO  o.c.s.SimpleSourceRefactoringService - Found 3 files to refactor.
11:42:56.290 [pool-5-thread-1] INFO  o.c.s.SimpleSourceRefactoringService - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageMonthly.java
11:42:58.809 [pool-5-thread-1] INFO  o.c.s.SimpleSourceRefactoringService - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageYearly.java
11:43:01.026 [pool-5-thread-1] INFO  o.c.s.SimpleSourceRefactoringService - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageReport.java
11:43:02.898 [tomcat-handler-2] INFO  o.c.s.SimpleSourceRefactoringService - Refactoring completed on refactor-a3b21c59-a5c2-4d62-a720-097a328dc894.
11:43:02.899 [tomcat-handler-2] INFO  org.cftoolsuite.client.GitClient - Push to remote not enabled!
11:43:02.899 [tomcat-handler-2] INFO  o.c.client.GithubPullRequestClient - Pull request not enabled!
```

#### Sample git log

```bash
❯ cd tmp/cf-butler
❯ git --no-pager log --max-count=2

commit da12636481d5cc6b20f8b78e4220e71e93c9014b (HEAD -> refactor-a3b21c59-a5c2-4d62-a720-097a328dc894)
Author: Chris Phillipson <cphillipson@pivotal.io>
Date:   Thu Aug 29 11:43:02 2024 -0700

    Refactored by org.cftoolsuite.service.SimpleSourceRefactoringService on 2024-08-29 11:43:02

commit 6562b05bf171d482c303895ad5206e1fc7962cae (tag: 2024.08.27, origin/main, main)
Author: Chris Phillipson <cphillipson@pivotal.io>
Date:   Tue Aug 27 08:02:12 2024 -0700

    Upgrade bcprov-jdk18on to 1.78.1
```

#### Validating each refactor request

To [validate what had been updated](https://stackoverflow.com/questions/9903541/finding-diff-between-current-and-last-version).

```bash
❯ cd tmp/cf-butler
❯ git show
```
> Use the up and down arrow keys to scroll through changes.  Type `:q` to exit.

or if you have a comparison tool like [Beyond Compare](https://www.scootersoftware.com/) installed, you could

```bash
❯ git config --global diff.tool bc
❯ git config --global difftool.bc.path /usr/bin/bcompare
❯ git config --global difftool.bc.trustExitCode true
```

then

```bash
❯ cd tmp/cf-butler
❯ git difftool --dir-diff refactor-{uuid} main
```
> Replace `{uuid}` with the suffix of the refactored branch
