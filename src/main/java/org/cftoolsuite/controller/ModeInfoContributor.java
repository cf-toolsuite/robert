package org.cftoolsuite.controller;

import java.util.Arrays;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ModeInfoContributor implements InfoContributor {

    private final Environment environment;

    public ModeInfoContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        boolean containsAdvanced = Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contains("advanced"));
        String mode = containsAdvanced ? "advanced" : "simple";
        builder.withDetail("mode", mode);
    }

}
