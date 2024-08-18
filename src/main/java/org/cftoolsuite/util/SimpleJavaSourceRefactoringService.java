package org.cftoolsuite.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SimpleJavaSourceRefactoringService implements RefactoringService {

    private static final String DEFAULT_PROMPT =
        """
        Assume the role and expertise of a Java and Spring developer aware of all projects in the Spring ecosystem and other third-party dependencies.
        You are asked to remove Lombok annotations and replace with equivalent plain Java source.  You are also asked to convert,
        where possible, Class to Record.  If a Class was annotated with Lombok's @Builder annotation, retain builder methods
        of the same signature as one would get with that annotation.  Do not pollute Class to Record conversions with getter and setter methods.
        Do not provide any additional messages or friendly responses, you should only provide the refactored source in the response.  Trim any whitespace.

        Refactor the Java source below:

        {source}
        """;

    private ChatClient client;
    private String prompt;

    public SimpleJavaSourceRefactoringService(ChatClient.Builder clientBuilder, @Value("#{systemProperties['prompt'] ?: ''}") String prompt) {
        this.client = clientBuilder.build();
        this.prompt = StringUtils.isNotBlank(prompt) && prompt.contains("{source}") ? prompt : DEFAULT_PROMPT;
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
}
