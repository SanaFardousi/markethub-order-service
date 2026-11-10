package com.markethub.orders.infrastructure.search;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.markethub.orders.infrastructure.persistence.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexer {

    public static final String ALIAS = "products";
    private static final String INDEX = "products-v1";
    private static final int BATCH_SIZE = 50;

    private final ElasticsearchClient client;
    private final ProductRepository products;

    /** Returns true when the index had to be created (first start, or its data was deleted). */
    public boolean ensureIndex() throws IOException {
        boolean created = false;
        if (!client.indices().exists(e -> e.index(INDEX)).value()) {
            try (InputStream mapping = new ClassPathResource("es/products.json").getInputStream()) {
                client.indices().create(c -> c.index(INDEX).withJson(mapping));
            }
            created = true;
        }
        if (!client.indices().existsAlias(a -> a.name(ALIAS)).value()) {
            client.indices().putAlias(p -> p.index(INDEX).name(ALIAS));
        }
        return created;
    }

}
