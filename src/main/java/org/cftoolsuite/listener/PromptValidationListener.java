package org.cftoolsuite.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class PromptValidationListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(PromptValidationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        String prompt = event.getApplicationContext().getBean("prompt", String.class);
        log.trace("Prompt: {}", prompt);
        if (!prompt.contains("{source}")) {
            log.error("Prompt must contain {source} placeholder!  Shutting down.");
            initiateShutdown(event.getApplicationContext(), 1);
        }
    }

    private void initiateShutdown(ApplicationContext context, int returnCode){
        SpringApplication.exit(context, () -> returnCode);
    }
}