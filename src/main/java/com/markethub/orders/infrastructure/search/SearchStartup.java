package com.markethub.orders.infrastructure.search;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchStartup implements SmartInitializingSingleton {

    private final ProductIndexer indexer;

    @Override
    public void afterSingletonsInstantiated() {
        try {
            if (indexer.ensureIndex()) {
                log.info("Created the search index and bulk indexed {} products", indexer.indexAll());
            }
        } catch (Exception e) {
            log.error("Could not prepare the search index. Search stays unavailable until the next restart.", e);
        }
    }
}
