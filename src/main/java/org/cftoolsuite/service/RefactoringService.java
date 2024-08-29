package org.cftoolsuite.service;

import java.io.IOException;

import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;

public interface RefactoringService {
    GitResponse refactor(GitRequest request) throws IOException;
}
