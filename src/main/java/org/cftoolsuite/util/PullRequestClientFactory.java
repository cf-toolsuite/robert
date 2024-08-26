package org.cftoolsuite.util;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class PullRequestClientFactory {

    private ListableBeanFactory beanFactory;

    public PullRequestClientFactory(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public PullRequestClient get(String uri) {
        return
            beanFactory
                .getBeansOfType(PullRequestClient.class)
                    .values()
                    .stream()
                        .filter(
                            client ->
                                uri
                                    .startsWith(client.uriPrefix())
                        )
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No client found for uri: " + uri));
    }

}
