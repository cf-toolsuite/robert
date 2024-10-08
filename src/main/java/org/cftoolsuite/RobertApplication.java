package org.cftoolsuite;

import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RobertApplication implements ApplicationListener<ContextRefreshedEvent> {

    private static final Set<String> vectorStoreProfiles = Set.of("chroma", "pgvector", "redis");

    public static void main(String[] args) {
        SpringApplication.run(RobertApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();
        activateAdditionalProfiles(env);
    }

    private void activateAdditionalProfiles(ConfigurableEnvironment env) {
        Set<String> activeProfiles = Set.of(env.getActiveProfiles());

        boolean hasMatchingProfile = activeProfiles.stream().anyMatch(vectorStoreProfiles::contains);

        if (hasMatchingProfile && !activeProfiles.contains("advanced")) {
            env.addActiveProfile("advanced");
        }
    }
}