# R*bert

## How to Build

```bash
./gradlew clean build
```

This builds a version of the utility that is compatible for use with [Groq Cloud](https://groq.com).  You will need to [obtain an API key](https://console.groq.com/docs/api-keys).


### Alternatives

The below represent the collection of Gradle [conditional dependencies](https://www.baeldung.com/gradle-conditional-dependencies#configuring-conditional-dependency) available in [build.gradle](../build.gradle)

#### [Ollama](https://ollama.com/)

Adds a dependency on [spring-ai-ollama-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)


```bash
./gradlew clean build -Pprofile=ollama
```
> Work with [your choice](https://github.com/ollama/ollama?tab=readme-ov-file#model-library) of Ollama LLMs


#### [Chroma](https://docs.trychroma.com/guides)

Adds dependencies on:

* [spring-ai-chroma-store-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/vectordbs/chroma.html)
* [spring-boot-docker-compose](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose)

```bash
./gradlew clean build -Pstore=chroma
```
