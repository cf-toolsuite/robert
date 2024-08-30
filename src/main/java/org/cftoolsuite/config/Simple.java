package org.cftoolsuite.config;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.client.GitClient;
import org.cftoolsuite.client.PullRequestClientFactory;
import org.cftoolsuite.service.RefactoringService;
import org.cftoolsuite.service.SimpleSourceRefactoringService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Profile({"!advanced"})
@Configuration
public class Simple {


    @Bean
    public String prompt(
        @Value("#{systemProperties['prompt'] ?: ''}") String prompt,
        @Value("classpath:ai/simple-refactor-request.st") Resource defaultPrompt) throws IOException {
        return StringUtils.isNotBlank(prompt) ? prompt :  defaultPrompt.getContentAsString(Charset.defaultCharset());
    }

    @Bean
    @DependsOn("prompt")
    @Description("A naive implementation of a function that returns refactoring results.")
    public RefactoringService refactoringService(
        ChatModel model,
        String prompt,
        GitClient gitClient,
        @Value("#{systemProperties['tpmDelay'] ?: '5'}") String tpmDelay,
        PullRequestClientFactory pullRequestClientFactory) {
        return new SimpleSourceRefactoringService(
            ChatClient.builder(model).build(), prompt, gitClient, tpmDelay, pullRequestClientFactory);
    }

}