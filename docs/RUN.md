# R*bert

* [How to Run with Gradle](#how-to-run-with-gradle)
  * [with OpenAI](#with-openai)
  * [with Groq Cloud](#with-groq-cloud)
  * [with Ollama](#with-ollama)
  * [with Vector database](#with-vector-database)
    * [Chroma](#chroma)
    * [PgVector](#pgvector)
    * [Redis Stack](#redis-stack)
    * [Weaviate](#weaviate)
  * [with alternate prompts](#with-alternate-prompts)
    * [Discovery](#discovery)
    * [Refactor](#refactor)
  * [override the token per minute delay](#override-the-token-per-minute-delay)
* [How to run on Tanzu Platform for Cloud Foundry](#how-to-run-on-tanzu-platform-for-cloud-foundry)
  * [Target a foundation](#target-a-foundation)
  * [Authenticate](#authenticate)
  * [Target space](#target-space)
  * [Verify services](#verify-services)
  * [Clone and build the app](#clone-and-build-the-app)
  * [Deploy](#deploy)
  * [Inspect and/or update the PgVector store database instance](#inspect-andor-update-the-pgvector-store-database-instance)
* [How to run on Kubernetes](#how-to-run-on-kubernetes)
  * [Build](#build)
  * [(Optional) Authenticate to a container image registry](#optional-authenticate-to-a-container-image-registry)
  * [(Optional) Push image to a container registry](#optional-push-image-to-a-container-registry)
  * [Target a cluster](#target-a-cluster)
  * [Prepare](#prepare)
  * [Apply](#apply)
  * [Setup port forwarding](#setup-port-forwarding)
  * [Teardown](#teardown)
* [How to run on Tanzu Platform for Kubernetes](#how-to-run-on-tanzu-platform-for-kubernetes)
  * [Clone this repo](#clone-this-repo)
  * [Initialize](#initialize)
    * [Configuring daemon builds](#configuring-daemon-builds)
    * [Configuring platform builds](#configuring-platform-builds)
    * [Validating build configuration](#validating-build-configuration)
  * [Pre-provision services](#pre-provision-services)
    * [Open AI](#open-ai)
    * [Weaviate Cloud](#weaviate-cloud)
  * [Define an Egress Point](#define-an-egress-point)
  * [Specify service bindings](#specify-service-bindings)
  * [Deploy services](#deploy-services)
  * [Deploy application with service bindings](#deploy-application-with-service-bindings)
  * [Establish a domain binding](#establish-a-domain-binding)
  * [Destroy the app and services](#destroy-the-app-and-services)

R*bert has two runtime modes of operation: `simple` and `advanced`, where the default mode is set to `simple`.

The `advanced` mode requires you to activate:

* a Gradle [project property](https://docs.gradle.org/current/userguide/migrating_from_maven.html#migmvn:profiles_and_properties) and
* Spring Boot [profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)

in order to package the appropriate runtime libraries and then appropriately configure runtime support a [VectorStore](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_available_implementations) and [EmbeddingModel](https://docs.spring.io/spring-ai/reference/api/embeddings.html#available-implementations).

Both modes work with a [ChatModel](https://docs.spring.io/spring-ai/reference/api/chatmodel.html#_available_implementations).  Currently model support is plumbed for Open AI (including Groq) and Ollama.

## How to Run with Gradle

### with OpenAI

Build and run a version of the utility that is compatible for use with [OpenAI](https://openai.com).  You will need to [obtain an API key](https://platform.openai.com/settings/profile?tab=api-keys).

Before launching the app:

* Create a `config` folder which would be a sibling of the `build` folder.  Create a file named `creds.yml` inside that folder.  Add your own API key into that file.

```yaml
spring:
  ai:
    openai:
      api-key: {REDACTED}
```
> Replace `{REDACTED}` above with your Groq Cloud API key

Open a terminal shell and execute

```bash
./gradlew build bootRun -Dspring.profiles.active=openai
```

### with Groq Cloud

Build and run a version of the utility that is compatible for use with [Groq Cloud](https://groq.com).  You will need to [obtain an API key](https://console.groq.com/keys).
Note that Groq does not currently have support for text embedding. So if you intend to run with the `advanced` Spring profile activated, you will also need to provide additional credentials

Before launching the app:

* Create a `config` folder which would be a sibling of the `build` folder.  Create a file named `creds.yml` inside that folder.  Add your own API key into that file.

```yaml
spring:
  ai:
    openai:
      api-key: {REDACTED-1}
      # Embedding configuration below only required when spring.profiles.active includes "advanced"
      embedding:
        api-key: {REDACTED-2}
        base_url: https://api.openai.com
```
> Replace `{REDACTED-1}` and `{REDACTED-2}` above with your Groq Cloud API and OpenAPI keys respectively.

Open a terminal shell and execute

```bash
./gradlew build bootRun -Dspring.profiles.active=groq-cloud
```

### with Ollama

Open a terminal shell and execute:

```bash
ollama pull mistral
ollama pull nomic-embed-text
ollama run mistral
```

Open another terminal shell and execute

```bash
./gradlew build bootRun -Dspring.profiles.active=ollama -Pmodel-api-provider=ollama
```
> You'll need to manually stop to the application with `Ctrl+C`

^ If you want to override the chat model you could add `-Dspring.ai.ollama.chat.options.model={model_name}` to the above and replace `{chat_model_name}` with a supported model.  Likewise, you may override the embedding model with `-Dspring.ai.ollama.embedding.options.model={embedding_model_name}`.

### with Vector database

This setup leverages Spring Boot's support for Docker Compose and launches either an instance of Chroma or PgVector for use by the VectorStore.  This mode activates Git repository ingestion and Document metadata enrichment for Java source files found.  It also activates the [DependencyAwareRefactoringService](../src/main/java/org/cftoolsuite/service/DependencyAwareRefactoringService.java).

A key thing to note is that **you must activate a combination** of Spring profiles, like:

* `docker` - required when you are running "off platform"
* an LLM provider (i.e., `openai`, `groq-cloud` or `ollama`)
* a Vector database provider (i.e., `chroma`, `pgvector`, `redis`, or `weaviate`)

and Gradle project properties, like:

* `-Pmodel-api-provider=ollama`
* `-Pvector-db-provider=chroma` or `-Pvector-db-provider=pgvector` or `-Pvector-db-provider=redis` or `-Pvector-db-provider=weaviate`

#### Chroma

```bash
./gradlew build bootRun -Dspring.profiles.active=docker,groq-cloud,chroma -Pvector-db-provider=chroma
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `groq-cloud` in `-Dspring.profiles.active` with `ollama`.

#### PgVector

```bash
./gradlew build bootRun -Dspring.profiles.active=docker,groq-cloud,pgvector -Pvector-db-provider=pgvector
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `groq-cloud` in `-Dspring.profiles.active` with `ollama`.

#### Redis Stack

```bash
./gradlew build bootRun -Dspring.profiles.active=docker,openai,redis -Pvector-db-provider=redis
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `-Dspring.profiles.active` with `ollama`.

#### Weaviate

```bash
./gradlew build bootRun -Dspring.profiles.active=docker,openai,weaviate -Pvector-db-provider=weaviate
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `openai` or `groq-cloud` in `-Dspring.profiles.active` with `ollama`.

### with alternate prompts

You may want to override the default, built-in discovery (seek) and refactor (prompt).  Keep in mind that you must add the named placeholders below to your custom prompts!

#### Discovery

Applies only when `advanced` Spring profile is activated

```bash
-Dseek="{discoveryPrompt} that finds all the candidates"
```

#### Refactor

Simple mode

```bash
-Dprompt="wonderful new {refactorPrompt} that reduces tech debt in your {source}"
```

or when the `advanced` Spring profile is activated, it should look a bit like:

```bash
-Dprompt="awesome new {refactorPrompt} that reduces tech debt in your {documents}"
```

If you have a sophisticated multi-line prompt you might want to read in the contents this way

```bash
-Dprompt="$(cat samples/refactor-lombok-slf4j.st)"
```
> You are certainly free to author your own prompt files, just replace the path above with your own

to invocations of

```bash
./gradlew bootRun
```

### override the token per minute delay

Each refactoring request is delayed by 5 seconds, so as not to exceed rate limits on hosted LLM platforms (see Groq Cloud's [limits](https://console.groq.com/settings/limits)).

If you wish to reduce or increase the delay (in seconds), you may add

```bash
-DtpmDelay={amount}
```
> Replace `{amount}` above with a whole integer value (e.g., 20)

to invocations of

```bash
./gradlew bootRun
```

## How to run on Tanzu Platform for Cloud Foundry

### Target a foundation

```bash
cf api {cloud_foundry_foundation_api_endpoint}
```

> Replace `{cloud_foundry_foundation_api_endpoint}` above with an API endpoint

Sample interaction

```bash
cf api api.sys.dhaka.cf-app.com
```

### Authenticate

Interactively

```bash
cf login
```

With single sign-on

```bash
cf login --sso
```

With a username and password

```bash
cf login -u {username} -p "{password}"
```

> Replace `{username}` and `{password}` above respectively with your account's username and password.

### Target space

If your user account has `OrgManager` and `SpaceManager` permissions, then you can create your own organization and space with

```bash
cf create-org {organization_name}
cf create-space -o {organization_name} {space_name}
```

> Replace `{organization_name}` and `{space_name}` above with names of your design

To target a space

```bash
cf target -o {organization_name} -s {space_name}
```

> Replace `{organization_name}` and `{space_name}` above with an existing organization and space your account has access to

Sample interaction

```bash
cf create-org zoolabs
cf create-space -o zoolabs dev
cf target -o zoolabs -s dev
```

### Verify services

Verify that the foundation has the service offerings required

```bash
cf m -e genai
cf m -e postgres
```

Sample interaction

```bash
❯ cf m -e genai
Getting service plan information for service offering genai in org zoolabs / space dev as chris.phillipson@broadcom.com...

broker: genai-service
   plan               description                                                                                       free or paid   costs
   llama3.1           Access to the llama3.1 model. Capabilities: chat, tools. Aliases: gpt-turbo-3.5.                  free
   llava              Access to the llava model. Capabilities: chat, vision.                                            free
   nomic-embed-text   Access to the nomic-embed-text model. Capabilities: embedding. Aliases: text-ada-embedding-002.   free

❯ cf m -e postgres
Getting service plan information for service offering postgres in org zoolabs / space dev as chris.phillipson@broadcom.com...

broker: postgres-odb
   plan                       description                             free or paid   costs
   on-demand-postgres-small   A single e2-micro with 2GB of storage   free
```

### Clone and build the app

```bash
gh repo clone cf-toolsuite/robert
cd robert
gradle build -Pvector-db-provider=pgvector
```

### Deploy

Take a look at the deployment script

```bash
cat deploy-on-tp4cf.sh
```

> Make any required edits to the environment variables for the services and plans.

Execute the deployment script

```bash
./deploy-on-tp4cf.sh setup
```

To teardown, execute

```bash
./deploy-on-tp4cf.sh teardown
```

### Inspect and/or update the PgVector store database instance

Create a service key for the service instance, with:

```bash
cf create-service-key robert-db cf-psql
```

Sample interaction

```bash
❯ cf create-service-key robert-db cf-psql
Creating service key cf-psql for service instance robert-db as chris.phillipson@broadcom.com...
OK

❯ cf service-key robert-db cf-psql
Getting key cf-psql for service instance robert-db as chris.phillipson@broadcom.com...

{
  "credentials": {
    "db": "postgres",
    "hosts": [
      "q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh"
    ],
    "jdbcUrl": "jdbc:postgresql://q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh:5432/postgres?user=pgadmin&password=Z8ybS105mdY7i6h923H4",
...
```

Open two terminal sessions.

In the first session, execute:

```bash
❯ cf ssh -L 55432:q-s0.postgres-instance.dhaka-services-subnet.service-instance-967aa687-1b73-4448-8505-dca0fa2ee079.bosh:5432 robert
vcap@128bacbc-b0f1-46b5-64cb-709c:~$
```

> We are creating a tunnel between the host and the service instance via the application. The host will listen on port 55432.

Switch to the second session, then execute:

```bash
❯ psql -U pgadmin -W postgres -h 127.0.0.1 -p 55432
Password:
```

Enter the password.  See that it is specified at the end fo the "jdbcUrl" JSON fragment above.

And you should see:

```bash
psql (12.9 (Ubuntu 12.9-0ubuntu0.20.04.1), server 15.6)
WARNING: psql major version 12, server major version 15.
         Some psql features might not work.
Type "help" for help.

postgres=#
```

From here you can show tables with `\dt`

```bash
postgres=# \dt
            List of relations
 Schema |     Name     | Type  |  Owner
--------+--------------+-------+---------
 public | vector_store | table | pgadmin
(1 row)
```

You can describe the table with `\d vector_store`

```bash
postgres=# \d vector_store
                     Table "public.vector_store"
  Column   |     Type     | Collation | Nullable |      Default
-----------+--------------+-----------+----------+--------------------
 id        | uuid         |           | not null | uuid_generate_v4()
 content   | text         |           |          |
 metadata  | json         |           |          |
 embedding | vector(1536) |           |          |
Indexes:
    "vector_store_pkey" PRIMARY KEY, btree (id)
    "spring_ai_vector_index" hnsw (embedding vector_cosine_ops)
```

And you can execute arbitrary SQL (e.g., `SELECT * from vector_store`).

If you need to ALTER the dimensions of the `embedding` column to adapt to the limits of an embedding model you chose, then you could, for example, execute:

```bash
-- Step 1: Drop the existing index
DROP INDEX IF EXISTS spring_ai_vector_index;

-- Step 2: Drop the existing column
ALTER TABLE public.vector_store DROP COLUMN embedding;

-- Step 3: Add the new column with the desired vector size
ALTER TABLE public.vector_store ADD COLUMN embedding vector(768);

-- Step 4: Recreate the index
CREATE INDEX spring_ai_vector_index ON public.vector_store USING hnsw (embedding vector_cosine_ops);
```

To exit, just type `exit`.


## How to run on Kubernetes

We're going to make use of the [Eclipse JKube Gradle plugin](https://eclipse.dev/jkube/docs/kubernetes-gradle-plugin/#getting-started).

To build targeting the appropriate, supporting, runtime infrastructure, you will need to choose:

* LLM provider
  * groq-cloud, openai
* Vector store
  * chroma, pgvector

### Build

To build a container image with Spring Boot, set the container image version, and assemble the required Kubernetes manifests for deployment, execute:

```bash
❯ gradle clean setVersion build bootBuildImage k8sResource -PnewVersion=$(date +"%Y.%m.%d") -Pvector-db-provider=chroma -Pjkube.environment=groq-cloud,chroma,observability --stacktrace
```

This will build and tag a container image using [Paketo Buildpacks](https://paketo.io/docs/concepts/buildpacks/) and produce a collection of manifests in `build/classes/META-INF/jkube`.

### (Optional) Authenticate to a container image registry

If you are a contributor with an account that has permissions to push updates to the container image, you will need to authenticate with the container image registry.

For [DockerHub](https://hub.docker.com/), you could execute:

```bash
echo "REPLACE_ME" | docker login docker.io -u cftoolsuite --password-stdin
```

For [GitHub CR](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry), you could execute:

```bash
echo "REPLACE_ME" | docker login ghcr.io -u cf-toolsuite --password-stdin
```

> Replace the password value `REPLACE_ME` above with a valid personal access token to DockerHub or GitHub CR.

### (Optional) Push image to a container registry

Here's how to push an update:

```bash
gradle k8sPush
```

### Target a cluster

You will need to establish a connection context to a cluster you have access to.

The simplest thing to do... is to launch a [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#creating-a-cluster) cluster.

```bash
kind create cluster
```

### Prepare

Consult GitHub CR for the latest available tagged image, [here](https://github.com/cf-toolsuite/robert/pkgs/container/robert).

Edit the `build/classes/java/main/META-INF/jkube/kubernetes/robert-deployment.yml` and `build/classes/java/main/META-INF/jkube/kubernetes/robert-service.yml` files

You should replace occurrences of `YYYY.MM.DD` (e.g., 2024.11.08) with the latest available tag, and save your changes.

> Note: the image tag in Github CR will have a slightly different format (e.g., 20241108.1429.08405)

Before deploying you will want to edit the contents of `build/classes/java/main/META-INF/jkube/kubernetes/spring-ai-creds-secret.yml`.

Back when you built the image and created the Kubernetes manifests, you had to supply a comma-separated `-Pjkube.environment=` set of argument values.

If that set contained `openai`, you would see the following fragment within the secret:

```yaml
stringData:
  creds.yml: |
    spring:
      ai:
        openai:
          api-key: REPLACE_WITH_OPENAI_API_KEY
```

> Your job is to replace the occurrence of `REPLACE_WITH_OPENAI_API_KEY` with valid API key value from Open AI.

If, however, that set contained `groq-cloud`, you would see the following fragment within the secret:

```yaml
stringData:
  creds.yml: |
    spring:
      ai:
        openai:
          api-key: REPLACE_WITH_GROQCLOUD_API_KEY
          embedding:
            api-key: REPLACE_WITH_OPENAI_API_KEY
            base_url: https://api.openai.com
```

> Your job is to replace the occurrences of values that start with `REPLACE_WITH` with valid API key values from Groq Cloud and Open AI respectively. The Open AI key-value is used for the embedding model as Groq Cloud does not have support for embedding models, yet.

### Apply

Finally, we can deploy the application and dependent runtime services to our Kubernetes cluster.

Do so, with:

```bash
gradle k8sApply -Pvector-db-provider=chroma -Pjkube.environment=openai,chroma,observability
```

or

```bash
kubectl apply -f build/classes/java/main/META-INF/jkube/kubernetes.yml
```

### Setup port forwarding

At this point you'd probably like to interact with robert, huh?  We need to setup port-forwarding, so execute:

```bash
kubectl port-forward service/robert 8080:8080
```

Then visit `http://localhost:8080/actuator/info` in your favorite browser.

Consult the [ENDPOINTS.md](ENDPOINTS.md) documentation to learn about what else you can do.

When you're done, revisit the terminal where you started port-forwarding and press `Ctrl+C`.

> Yeah, this only gets you so far.  For a more production-ready footprint, there's quite a bit more work involved.  But this suffices for an inner-loop development experience.

### Teardown

```bash
gradle k8sUndeploy -Pvector-db-provider=chroma -Pjkube.environment=openai,chroma,observability
```

or

```bash
kubectl delete -f build/classes/java/main/META-INF/jkube/kubernetes.yml
```

And if you launched a Kind cluster earlier, don't forget to tear it down with:

```bash
kind delete cluster
```

## How to run on Tanzu Platform for Kubernetes

Consider this a Quick Start guide to getting `robert` deployed using the [tanzu](https://docs.vmware.com/en/VMware-Tanzu-Application-Platform/1.12/tap/install-tanzu-cli.html) CLI.

We'll be focused on a subset of commands to get the job done.  That said, you will likely need to work with a Platform Engineer within your enterprise to pre-provision an environment for your use.

This [Github gist](https://gist.github.com/pacphi/b9a7bb0f9538db1d11d1671d8a2b5566) should give you sense of how to get started with infrastructure provisioning and operational concerns, before attempting to deploy.

### Clone this repo

```bash
gh repo clone cf-toolsuite/robert
```

### Initialize

> We're going to assume that your account is a member of an organization and has appropriate access-level permissions to work with an existing project and space(s).

Login, set a project and space.

```bash
tanzu login
tanzu project list
tanzu project use AMER-West
tanzu space list
tanzu space use cphillipson-sbx
```

> You will set a different `project` and `space`.  The above is just illustrative of what you'll need to do to target where you'll deploy your own instance of this application.

Set the context for `kubectl`, just in case you need to inspect resources.

```bash
tanzu context current
```

**Sample interaction**

```bash
❯ tanzu context current
  Name:            sa-tanzu-platform
  Type:            tanzu
  Organization:    sa-tanzu-platform (77aee83b-308f-4c8e-b9c4-3f7a6f19ba75)
  Project:         AMER-West (3b65ba5e-52a4-4666-ad29-4eefab93127b)
  Space:           cphillipson-sbx
  Kube Config:     /home/cphillipson/.config/tanzu/kube/config
  Kube Context:    tanzu-cli-sa-tanzu-platform:AMER-West:cphillipson-sbx
```

Then

```bash
# Use the value after "Kube Config:"
# Likely this will work consistently for you
export KUBECONFIG=$HOME/.config/tanzu/kube/config
```

Now, let's jump into the root-level directory of the Git repository's project we cloned earlier, create a new branch, and freshly initialize Tanzu application configuration.

```bash
cd robert
git checkout -b tp4k8s-experiment
tanzu app init -y
```

We'll also need to remove any large files or sensitive configuration files.

```
du -sh * -c
./prune.sh
```

Edit the file `.tanzu/config/robert.yml`.

It should look like this after editing.  Save your work.

```yaml
apiVersion: apps.tanzu.vmware.com/v1
kind: ContainerApp
metadata:
  name: robert
spec:
  nonSecretEnv:
    - name: JAVA_TOOL_OPTIONS
      value: "-Djava.security.egd=file:///dev/urandom -XX:+UseZGC -XX:+UseStringDeduplication"
    - name: SPRING_PROFILES_ACTIVE
      value: "default,cloud,openai,weaviate"
  build:
    nonSecretEnv:
    - name: BP_JVM_VERSION
      value: "21"
    - name: BP_GRADLE_BUILD_ARGUMENTS
      value: "clean build -Pvector-db-provider=weaviate"
    buildpacks: {}
    path: ../..
  contact:
    team: cftoolsuite
  ports:
  - name: main
    port: 8080
  probes:
    liveness:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
        scheme: HTTP
    readiness:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
        scheme: HTTP
    startup:
      failureThreshold: 120
      httpGet:
        path: /actuator/health/readiness
        port: 8080
        scheme: HTTP
      initialDelaySeconds: 2
      periodSeconds: 2
```

#### Configuring daemon builds

```bash
tanzu build config \
  --build-plan-source-type ucp \
  --containerapp-registry docker.io/{contact.team}/{name} \
  --build-plan-source custom-build-plan-ingressv2  \
  --build-engine=daemon
```

Builds will be performed locally.  (Docker must be installed).  We are targeting Dockerhub as the container image registry.  If you wish to target another registry provider you would have to change the prefix value of `docker.io` above to something else.  Pay attention to `{contact.team}`.  In the `ContainerApp` resource definition above, you will have to change `cftoolsuite` to an existing repository name in your registry.

You will also have to authenticate with the registry, e.g.

```bash
export REGISTRY_USERNAME=cftoolsuite
export REGISTRY_PASSWORD=xxx
export REGISTRY_HOST=docker.io
echo $REGISTRY_PASSWORD | docker login $REGISTRY_HOST -u $REGISTRY_USERNAME --password-stdin
```

> Replace the values of `REGISTRY_` environment variables above as appropriate.

By the way, whatever container image registry provider you choose, make sure to restrict access to the repository.  If you're using DockerHub, set the visibility of your repository to private.

> If your app will work with a private registry, then your Platform Engineer will have to have had to configure the [Registry Credentials Pull Only Installer](https://www.platform.tanzu.broadcom.com/hub/application-engine/capabilities/registry-pull-only-credentials-installer.tanzu.vmware.com/details).

<details>

<summary>Working with a container registry hosted on Github</summary>

Alternatively, if you intend to use [Github](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry) as a container image registry provider for your repository, you could authenticate to the registry with

```bash
export REGISTRY_USERNAME=cf-toolsuite
export REGISTRY_PASSWORD=xxx
export REGISTRY_HOST=ghcr.io
echo $REGISTRY_PASSWORD | docker login $REGISTRY_HOST -u $REGISTRY_USERNAME --password-stdin
```

then update the build configuration to be

```bash
tanzu build config \
  --build-plan-source-type ucp \
  --containerapp-registry ghcr.io/{contact.team}/{name} \
  --build-plan-source custom-build-plan-ingressv2  \
  --build-engine=daemon
```

and finally, make sure that `contact.name` in the `ContainerApp` is updated to be `cf-toolsuite` which matches the organization name for the Github repository.

> The first time you build and publish the container image, if you do not want to have to configure `Registry Credentials Pull Only Installer`, you will need to visit the `Package settings`, then set the visibility of the package to `Public`.

</details>

#### Configuring platform builds

```bash
tanzu build config \
  --build-plan-source-type ucp \
  --containerapp-registry us-west1-docker.pkg.dev/fe-cpage/west-sa-build-registry/{contact.team}/{name} \
  --build-plan-source custom-build-plan-ingressv2 \
  --build-engine=platform
```

A benefit of platform builds is that they occur on-platform.  (Therefore, Docker does not need to be installed).  We will assume that a Platform Engineer has set this up on our behalf.

> You will likely need to change `us-west1-docker.pkg.dev/fe-cpage/west-sa-build-registry` above to an appropriate prefix targeting a shared container image registry.

#### Validating build configuration

For daemon builds, e.g.

```bash
❯ tanzu build config view
Using config file: /home/cphillipson/.config/tanzu/build/config.yaml
Success: Getting config
buildengine: daemon
buildPlanSource: custom-build-plan-ingressv2
buildPlanSourceType: ucp
containerAppRegistry: docker.io/{contact.team}/{name}
experimentalFeatures: false
```

### Pre-provision services

Create a new sibling directory of `.tanzu/config` to contain service manifests.

```bash
mkdir .tanzu/service
```

Place yourself in the `.tanzu/service` directory.  We'll create `PreProvisionedService` and `Secret` manifests for a handful of the off-platform services that `robert` will need to interact with.

```bash

cd .tanzu/service
```

#### Open AI

Create a file named `openai.yml`, adjust and save the content below:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: openai-creds
type: servicebinding.io/ai
stringData:
  uri: CHANGE_ME
  api-key: CHANGE_ME
  provider: openai
  type: openai

---
apiVersion: services.tanzu.vmware.com/v1
kind: PreProvisionedService
metadata:
  name: openai
spec:
  bindingConnectors:
  - name: main
    description: Open AI credentials
    type: openai
    secretRef:
      name: openai-creds
```

> You will need to replace occurrences of `CHANGE_ME` above with your own `uri` and `api-key` values that will authenticate and authorize a connection to your account on the Open AI platform.

#### Weaviate Cloud

Create a file named `weaviate-cloud.yml`, adjust and save the content below:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: weaviate-cloud-creds
type: servicebinding.io/ai
stringData:
  uri: CHANGE_ME
  api-key: CHANGE_ME
  provider: weaviate-cloud
  type: weaviate-cloud

---
apiVersion: services.tanzu.vmware.com/v1
kind: PreProvisionedService
metadata:
  name: weaviate-cloud
spec:
  bindingConnectors:
  - name: main
    description: Weaviate Cloud credentials
    type: weaviate-cloud
    secretRef:
      name: weaviate-cloud-creds
```

> You will need to replace occurrences of `CHANGE_ME` above with your own `uri` and `api-key` values that will authenticate and authorize a connection to the instance of Weaviate you are hosting on [Weaviate Cloud](https://console.weaviate.io).

### Define an Egress Point

Create a file named `robert-services-egress.yml`, adjust and save the content below:

```bash
apiVersion: networking.tanzu.vmware.com/v1alpha1
kind: EgressPoint
metadata:
   name: robert-services-egress
spec:
  targets:
  # Open AI host
  - hosts:
    - api.openai.com
    port:
      number: 443
      protocol: HTTPS
  # Weaviate Cloud host
  - hosts:
    - CHANGE_ME.gcp.weaviate.cloud
    port:
      number: 443
      protocol: HTTPS
```

> You will need to replace occurrences of `CHANGE_ME` above with your own.  Note the `EgressPoint`'s `hosts` values above should be the same as those for the `Secret`'s `uri` values but without the scheme (i.e., do not include `https://`).

### Specify service bindings

Change directories again.  Place yourself back into the directory containing `robert.yml`.

```bash
cd ../config
```

Create another file named `robert-service-bindings.yml` and save the content below into it.  This file should live in same directory as the services and the application.

```yaml
apiVersion: services.tanzu.vmware.com/v1
kind: ServiceBinding
metadata:
  name: openai-service-binding
spec:
  targetRef:
    apiGroup: apps.tanzu.vmware.com
    kind: ContainerApp
    name: robert

  serviceRef:
    apiGroup: services.tanzu.vmware.com
    kind: PreProvisionedService
    name: openai
    connectorName: main

---
apiVersion: services.tanzu.vmware.com/v1
kind: ServiceBinding
metadata:
  name: weaviate-cloud-service-binding
spec:
  targetRef:
    apiGroup: apps.tanzu.vmware.com
    kind: ContainerApp
    name: robert

  serviceRef:
    apiGroup: services.tanzu.vmware.com
    kind: PreProvisionedService
    name: weaviate-cloud
    connectorName: main
```

### Deploy services

Let's place ourselves back into the root-level directory

```bash
cd ../..
```

then execute

```bash
tanzu deploy --only .tanzu/service -y
```

### Create and publish package to container image registry repository

```bash
tanzu build -o .tanzu/build
```

### Deploy application with service bindings

```bash
tanzu deploy --from-build .tanzu/build -y
```

Here's a few optional commands you could run afterwards to check on the state of deployment

```bash
tanzu app get robert
tanzu app logs robert --lines -1
```

### Establish a domain binding

```bash
tanzu domain-binding create robert --domain robert.sbx.tpk8s.cloudmonk.me --entrypoint main --port 443
```

> Replace the portion of the value of `--domain` before the application name above with your own sub-domain (or with one your Platform Engineer setup on your behalf).

Checkout [ENDPOINTS.md](ENDPOINTS.md) to see what you can do.

### Destroy the app and services

```bash
kubectl delete -f .tanzu/service
tanzu app delete robert -y
```
