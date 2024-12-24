package org.cftoolsuite.service;

import org.apache.commons.lang3.StringUtils;
import org.cftoolsuite.domain.GitRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.CollectionUtils;

class SearchAssistant {

    static Filter.Expression assembleFilterExpression(GitRequest request, String origin, String latestCommit) {
        var b = new FilterExpressionBuilder();
        var commit = StringUtils.isBlank(request.commit()) ? latestCommit : request.commit();
        if (CollectionUtils.isEmpty(request.allowedExtensions())) {
            return b.and(b.eq("commit", commit), b.eq("origin", origin)).build();
        }
        Object[] fileExtensions = request.allowedExtensions().toArray(String[]::new);
        return
            b.and(
                b.and(
                    b.eq("commit", commit), b.eq("origin", origin)
                ),
                b.in("file-extension", fileExtensions)
            )
            .build();
    }
}
