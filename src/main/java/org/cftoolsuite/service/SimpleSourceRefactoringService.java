package org.cftoolsuite.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SimpleSourceRefactoringService implements RefactoringService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSourceRefactoringService.class);

    private static final String DEFAULT_PROMPT =
        """
        Assume the role and expertise of a Java and Spring developer aware of all projects in the Spring ecosystem and other third-party dependencies.
        You are asked to remove Lombok annotations and replace with equivalent plain Java source.  You are also asked to convert,
        where possible, Class to Record.  If a Class was annotated with Lombok's @Builder annotation, retain builder methods
        of the same signature as one would get with that annotation.  Do not pollute Class to Record conversions with getter and setter methods.
        Do not include Markdown occurrences like ``` and trim any whitespace in response.
        Furthermore, do not provide any additional messages or friendly explanations in response.
        You should only provide the refactored source in the response.

        Refactor the Java source below:

        {source}
        """;

    private ApplicationContext context;
    private ChatClient client;
    private String prompt;

    public SimpleSourceRefactoringService(
        ApplicationContext context,
        ChatClient.Builder clientBuilder,
        @Value("#{systemProperties['prompt'] ?: ''}") String prompt
    ) {
        this.context = context;
        this.client = clientBuilder.build();
        this.prompt = StringUtils.isNotBlank(prompt) ? prompt : DEFAULT_PROMPT;
        log.trace("Initializing {} with {}", SimpleSourceRefactoringService.class.getName(), this.prompt);
        if (!this.prompt.contains("{source}")) {
            log.error("Prompt must contain {source} placeholder!  Shutting down.");
            initiateShutdown(1);
        }
    }

    public String refactor(String source) {
        return client.prompt()
				.user(
                    u ->
                        u
                        .text(prompt)
                        .param("source", source)
                )
 				.call()
                .content();
    }

    private void initiateShutdown(int returnCode){
        SpringApplication.exit(this.context, () -> returnCode);
    }

}
