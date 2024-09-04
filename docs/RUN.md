# R*bert

R*bert has two runtime modes of operation: `simple` and `advanced`, where the default mode is set to `simple`.

The `advanced` mode requires you to activate:

* a Gradle [project property](https://docs.gradle.org/current/userguide/migrating_from_maven.html#migmvn:profiles_and_properties) and
* Spring Boot [profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)

in order to package the appropriate runtime libraries in order to support a [VectorStore](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_available_implementations) and [EmbeddingModel](https://docs.spring.io/spring-ai/reference/api/embeddings.html#available-implementations).

Both modes work with a [ChatModel](https://docs.spring.io/spring-ai/reference/api/chatmodel.html#_available_implementations).  Currently model support is plumbed for Open AI (including Groq) and Ollama.

## How to Run with Gradle

### with Groq Cloud

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
./gradlew bootRun
```

### with Ollama

Open a terminal shell and execute

```bash
ollama pull llama3.1:70b
ollama run llama3.1:70b
```

Open another terminal shell and execute

```bash
./gradlew bootRun -Dspring.profiles.active=ollama
```
> You'll need to manually stop to the application with `Ctrl+C`

^ If you want to override the model being used you could add `-Dspring.ai.ollama.chat.options.model={model_name}` to the above and replace `{model_name}` with a supported model.

### with Vector database

This setup leverages Spring Boot's support for Docker Compose and launches either an instance of Chroma, PgVector, or PostgresML for use by the VectorStore.  This mode activates Git repository ingestion and Document metadata enrichment for Java source files found.  It also activates the [DependencyAwareRefactoringService](../src/main/java/org/cftoolsuite/service/DependencyAwareRefactoringService.java).

#### Chroma

```bash
./gradlew build bootRun -Dspring.profiles.active=advanced,groq-cloud,chroma -Pstore=chroma
```
> You also have the option of running with `ollama`.

#### PgVector

```bash
./gradlew build bootRun -Dspring.profiles.active=advanced,groq-cloud,pgvector -Pstore=pgvector
```
> You also have the option of running with `ollama`.

#### PostgresML

```bash
./gradlew build bootRun -Dspring.profiles.active=advanced,groq-cloud,postgresml -Pstore=postgresml
```
> You also have the option of running with `ollama`.

A key thing to note is that **you must activate a combination** of Spring profiles:

* `advanced`
* an LLM provider (i.e., `groq-cloud` or `ollama`)
* a store (i.e., `chroma`, `pgvector`, or `postgresml`)

and a Gradle project property, either of:

* `chroma`
* `postgres`

### with alternate prompt

You may want to override the default, built-in refactoring prompt.  To do that make sure the text of your new prompt contains a `{source}` placeholder, then append, e.g.,

```bash
-Dprompt="wonderful new prompt that reduces tech debt in your {source}"
```

or if you have a sophisticated multi-line prompt you might want to read in the contents this way

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
