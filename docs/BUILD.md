# R*bert

## How to Build

```
./gradlew clean build
```

This builds a version of the utility that is compatible for use with [Groq Cloud](https://groq.com).  You will need to [obtain an API key](https://console.groq.com/docs/api-keys).


### Alternatives

The below represent a collection of Gradle profiles available in build.gradle

* [Ollama](https://ollama.com/)
  * adds a dependency on [spring-ai-ollama-spring-boot-starter](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)


```
./gradlew clean build -Pprofile=ollama
```
> Work with [your choice](https://github.com/ollama/ollama?tab=readme-ov-file#model-library) of Ollama LLMs
