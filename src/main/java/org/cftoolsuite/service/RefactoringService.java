package org.cftoolsuite.service;

import java.util.function.Function;

import org.cftoolsuite.domain.GitRequest;
import org.cftoolsuite.domain.GitResponse;

public interface RefactoringService extends Function<GitRequest, GitResponse>{}
