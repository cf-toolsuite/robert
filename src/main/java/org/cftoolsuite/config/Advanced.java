package org.cftoolsuite.config;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.etl.GitRepositoryIngester;
import org.cftoolsuite.etl.JavaSourceMetadataEnricher;
import org.cftoolsuite.service.chat.ChatService;
import org.cftoolsuite.service.DependencyAwareRefactoringService;
import org.cftoolsuite.service.RefactoringService;
import org.cftoolsuite.service.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

@Profile({"advanced"})
@Configuration
public class Advanced {

    @Bean
    public String prompt(
        @Value("#{systemProperties['prompt'] ?: ''}") String prompt,
        @Value("classpath:ai/advanced-refactor-request.st") Resource defaultPrompt) throws IOException {
        return StringUtils.isNotBlank(prompt) ? prompt :  defaultPrompt.getContentAsString(Charset.defaultCharset());
    }

    @Bean
    public String dependenciesManagementStanza() {
        return """
            Take note of any dependent relationships in source.
            Update method calls in modified source if appropriate.
            """;
    }

    @Bean
    public String seek(
        @Value("#{systemProperties['seek'] ?: ''}") String seek,
        @Value("classpath:ai/advanced-seek-request.st") Resource defaultSeek) throws IOException {
        return StringUtils.isNotBlank(seek) ? seek :  defaultSeek.getContentAsString(Charset.defaultCharset());
    }

    @Bean
    @DependsOn({"prompt", "seek", "dependenciesManagementStanza"})
    @Description("A sophisticated implementation of a function that returns refactoring results.")
    public RefactoringService refactoringService(
        ChatModel model,
        String seek,
        String prompt,
        String dependenciesManagementStanza,
        GitClient gitClient,
        @Value("#{systemProperties['tpmDelay'] ?: '5'}") String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory,
        VectorStore store) {
        return new DependencyAwareRefactoringService(
            ChatClient.builder(model).build(), seek, prompt, dependenciesManagementStanza, gitClient, tpmDelay, pullRequestClientFactory, store);
    }

    @Bean
    public GitRepositoryIngester gitRepositoryIngester(
        VectorStore store,
        GitClient client,
        ListableBeanFactory beanFactory) {
        return new GitRepositoryIngester(store, client, beanFactory);
    }

    @Bean
    public JavaSourceMetadataEnricher javaSourceMetadataEnricher(ObjectMapper objectMapper) {
        return new JavaSourceMetadataEnricher(objectMapper);
    }

    @Bean
    public SearchService searchService(VectorStore store, GitClient gitClient) {
        return new SearchService(store, gitClient);
    }

    @Bean ChatService chatService(ChatModel model, VectorStore vectorStore) {
        return new ChatService(model, vectorStore);
    }

}
