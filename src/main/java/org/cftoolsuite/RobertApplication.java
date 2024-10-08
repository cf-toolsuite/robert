package org.cftoolsuite;

import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RobertApplication {

    private static final Set<String> vectorStoreProfiles = Set.of("chroma", "pgvector", "redis");

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RobertApplication.class);
        activateAdditionalProfiles(app);
        app.run(args);
    }

    private static void activateAdditionalProfiles(SpringApplication app) {
		String activeProfiles = System.getProperty("spring.profiles.active");

		if (activeProfiles == null) {
            activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }

        Set<String> activeProfilesSet = Set.of(activeProfiles.split(","));

        boolean hasMatchingProfile = activeProfilesSet.stream().anyMatch(vectorStoreProfiles::contains);

        if (hasMatchingProfile && !activeProfiles.contains("advanced")) {
            app.setAdditionalProfiles("advanced");
        }
	}
}
