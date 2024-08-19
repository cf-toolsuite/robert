# R*bert

## How to Run with Gradle

### with Groq Cloud

Before launching the app:

* Create a `config` folder which would be a sibling of the `build` folder.  Create a file named `creds.yml` inside that folder.  Add your own API key into that file.

```
spring:
  ai:
    openai:
      api-key: {REDACTED}
```
> Replace `{REDACTED}` above with your Groq Cloud API key

Open a terminal shell and execute

```
./gradlew bootRun
```

### with Ollama

Open a terminal shell and execute

```
ollama pull llama3.1:70b
ollama run llama3.1:70b
```

Open another terminal shell and execute

```
./gradlew bootRun -Dspring.profiles.active=ollama
```
> You'll need to manually stop to the application with `Ctrl+C`

^ If you want to override the model being used you could add `-Dspring.ai.ollama.chat.options.model={model_name}` to the above and replace `{model_name}` with a supported model.

### with alternate prompt

You may want to override the default, built-in refactoring prompt.  To do that make sure the text of your new prompt contains a `{source}` placeholder, then append, e.g.,

```
-Dprompt="wonderful new prompt that reduces tech debt in your {source}"
```

or if you have a sophisticated multi-line prompt you might want to read in the contents this way

```
-Dprompt="$(cat samples/refactor-lombok-slf4j.prompt)"
```
> You are certainly free to author your own prompt files, just replace the path above with your own

to invocations of

```
./gradlew bootRun
```

### override the token per minute delay

Each refactoring request is delayed by 5 seconds, so as not to exceed rate limits on hosted LLM platforms (see Groq Cloud's [limits](https://console.groq.com/settings/limits)).

If you wish to reduce or increase the delay (in seconds), you may add

```
-DtpmDelay={amount}
```
> Replace `{amount}` above with a whole integer value (e.g., 20)

to invocations of

```
./gradlew bootRun
```
