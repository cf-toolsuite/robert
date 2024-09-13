package org.cftoolsuite.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Profile("advanced")
public class AdvancedPromptValidationListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AdvancedPromptValidationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        boolean shutdown = false;
        String seek = event.getApplicationContext().getBean("seek", String.class);
        String prompt = event.getApplicationContext().getBean("prompt", String.class);
        log.trace("Discovery prompt: {}", seek);
        log.trace("Refactor prompt: {}", prompt);
        if (!seek.contains("{discoveryPrompt}")) {
            log.error("Discovery prompt must contain {discoveryPrompt} placeholder! Shutting down.");
            shutdown = true;
        }
        if (!prompt.contains("{documents}") || !prompt.contains("{refactorPrompt}")) {
            log.error("Refactor prompt must contain both {refactorPrompt} and {documents} placeholders! Shutting down.");
            shutdown = true;
        }
        if (shutdown) { initiateShutdown(event.getApplicationContext(), 1); }
    }

    private void initiateShutdown(ApplicationContext context, int returnCode){
        SpringApplication.exit(context, () -> returnCode);
    }
}