package org.cftoolsuite.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.cftoolsuite.config.AllowedLanguageFileExtensions;
import org.cftoolsuite.domain.LanguageExtensions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LanguagesController {

    private final AllowedLanguageFileExtensions config;

    public LanguagesController(AllowedLanguageFileExtensions config) {
        this.config = config;
    }

    @GetMapping("/language-extensions")
    public ResponseEntity<Set<LanguageExtensions>> languageExtensions() {
        return ResponseEntity.ok(config.getAllowedExtensions().entrySet().stream()
            .map(entry -> new LanguageExtensions(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet()));
    }
}
