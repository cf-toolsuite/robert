# R*bert

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
./gradlew bootRun
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
./gradlew bootRun
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

#### Chroma

```bash
./gradlew build bootRun -Dspring.profiles.active=advanced,groq-cloud,chroma -Pvector-db-provider=chroma
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `groq-cloud` in `-Dspring.profiles.active` with `ollama`.

#### PgVector

```bash
./gradlew build bootRun -Dspring.profiles.active=advanced,groq-cloud,pgvector -Pvector-db-provider=pgvector
```
> You also have the option of building with `-Pmodel-api-provider=ollama` then replacing `groq-cloud` in `-Dspring.profiles.active` with `ollama`.


A key thing to note is that **you must activate a combination** of Spring profiles, like:

* `advanced`
* an LLM provider (i.e., `groq-cloud` or `ollama`)
* a Vector database provider (i.e., `chroma` or `pgvector`)

and Gradle project properties, like:

* `-Pmodel-api-provider=ollama`
* `-Pvector-db-provider=chroma` or `-Pvector-db-provider=pgvector`

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
