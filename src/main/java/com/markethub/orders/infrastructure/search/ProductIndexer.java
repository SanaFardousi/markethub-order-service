package com.markethub.orders.infrastructure.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.markethub.orders.domain.model.Product;
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

    public int indexAll() throws IOException {
        int total = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));
        Page<Product> page;
        do {
            page = products.findAll(pageable);
            if (!page.isEmpty()) {
                List<BulkOperation> operations = page.getContent().stream()
                    .map(ProductDocument::from)
                    .map(d -> BulkOperation.of(op -> op.index(i -> i
                        .index(ALIAS).id(d.sku()).document(d)
                        .version(d.version()).versionType(VersionType.External))))
                    .toList();
                BulkResponse response = client.bulk(b -> b.operations(operations));
                report(response);
                total += operations.size();
            }
            pageable = page.nextPageable();
        } while (page.hasNext());
        return total;
    }

    private void report(BulkResponse response) {
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null && item.status() != 409) {
                log.warn("Could not index {}: {}", item.id(), item.error().reason());
            }
        }
    }

    public void index(ProductDocument doc) throws IOException {
        try {
            client.index(i -> i.index(ALIAS).id(doc.sku()).document(doc)
                .version(doc.version()).versionType(VersionType.External));
        } catch (ElasticsearchException e) {
            if (e.status() == 409) {
                log.info("Ignoring stale or repeated event for {} (version {})", doc.sku(), doc.version());
                return;
            }
            throw e;
        }
    }
}
