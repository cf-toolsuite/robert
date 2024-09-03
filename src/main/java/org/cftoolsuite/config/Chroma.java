package org.cftoolsuite.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.PropertyResolver;

@Profile({"chroma"})
@Configuration
public class Chroma {

    @Bean
    public EmbeddingModel embeddingModel(PropertyResolver resolver) {
        return new OpenAiEmbeddingModel(
            new OpenAiApi(resolver.getProperty("spring.ai.openai.embedding.base_url"), resolver.getProperty("spring.ai.openai.embedding.api-key"))
        );
    }
}
