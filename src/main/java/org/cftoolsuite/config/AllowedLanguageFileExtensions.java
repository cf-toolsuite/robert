package org.cftoolsuite.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AllowedLanguageFileExtensions {

    private static final Logger log = LoggerFactory.getLogger(AllowedLanguageFileExtensions.class);

    private Map<String, String> allowedExtensions = new HashMap<>();

    @Value("#{systemProperties['allowed_language_file_extensions'] != null ? 'file:' + systemProperties['allowed_language_file_extensions'] : ''}")
    private Resource overrideResource;

    @PostConstruct
    public void init() {
    if (overrideResource != null && overrideResource.exists()) {
            try {
                Properties props = new Properties();
                props.load(overrideResource.getInputStream());
                for (String key : props.stringPropertyNames()) {
                    allowedExtensions.put(key, props.getProperty(key));
                }
            } catch (IOException e) {
                log.warn("Error loading override file.  Falling back to defaults.", e);
            }
        }
    }

    public Map<String, String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Map<String, String> allowedExtensions) {
        this.allowedExtensions.clear();
        this.allowedExtensions.putAll(allowedExtensions);
    }
}