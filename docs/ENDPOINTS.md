# R*bert

* [Endpoints](#endpoints)
  * [Clone](#clone)
  * [Refactor](#refactor)

## Endpoints

Both endpoints below work with [GitSettings.java](../src/main/java/org/cftoolsuite/util/GitSettings.java).

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
Content-Length: 0
Date: Sun, 18 Aug 2024 04:49:35 GMT
Keep-Alive: timeout=60
```

#### Sample log output

```bash
21:49:22.783 [main] INFO  org.cftoolsuite.RobertApplication - Started RobertApplication in 1.911 seconds (process running for 2.158)
21:49:28.657 [http-nio-8080-exec-1] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring DispatcherServlet 'dispatcherServlet'
21:49:28.658 [http-nio-8080-exec-1] INFO  o.s.web.servlet.DispatcherServlet - Initializing Servlet 'dispatcherServlet'
21:49:28.659 [http-nio-8080-exec-1] INFO  o.s.web.servlet.DispatcherServlet - Completed initialization in 0 ms
21:49:30.796 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - Cloned Repository[/home/cphillipson/Documents/development/pivotal/cf/robert/tmp/cf-butler/.git]
21:49:30.801 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - Latest commit with id a5ea2b36a5fe094925337a5fe5f259ca9dd4313e was made Fri Aug 16 19:39:26 PDT 2024 on Merge branch 'main' of https://github.com/cf-toolsuite/cf-butler by Chris Phillipson
21:49:30.802 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageMonthly.java
21:49:30.802 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageReport.java
21:49:30.802 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - -- Obtaining contents of src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageYearly.java
21:49:30.803 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitController - Found 3 files to refactor.
21:49:30.803 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitController - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageMonthly.java
21:49:32.798 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitController - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageYearly.java
21:49:34.195 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitController - -- Attempting to refactor src/main/java/org/cftoolsuite/cfapp/domain/accounting/application/AppUsageReport.java
21:49:35.359 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitController - Refactoring completed on refactor-647ee41c-8ff1-4c40-bfcb-2e5567193850.
21:49:35.360 [http-nio-8080-exec-1] INFO  org.cftoolsuite.util.GitClient - Push to remote not enabled!
...
```

#### Sample git log

```bash
❯ cd tmp/cf-butler
❯ git --no-pager log --max-count=2
commit 0a4008ee85162580c9627eefe1eee0c7b9dc68d0 (HEAD -> refactor-647ee41c-8ff1-4c40-bfcb-2e5567193850)
Author: Chris Phillipson <cphillipson@pivotal.io>
Date:   Sat Aug 17 21:49:35 2024 -0700

    Refactored by org.cftoolsuite.util.SimpleJavaSourceRefactoringService on 2024-08-17 21:49:35

commit a5ea2b36a5fe094925337a5fe5f259ca9dd4313e (origin/main, main)
Merge: 0b3a283 6425425
Author: Chris Phillipson <cphillipson@pivotal.io>
Date:   Fri Aug 16 19:39:26 2024 -0700

    Merge branch 'main' of https://github.com/cf-toolsuite/cf-butler
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
