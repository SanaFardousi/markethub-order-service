package com.markethub.orders.infrastructure.kafka;

import com.markethub.orders.infrastructure.search.ProductDocument;
import com.markethub.orders.infrastructure.search.ProductIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ProductIndexer indexer;

    @KafkaListener(topics = Topics.PRODUCTS)
    public void onProductEvent(String json) throws IOException {
        ProductEvent event = JSON.readValue(json, ProductEvent.class);
        ProductEvent.Payload p = event.payload();
        log.info("Received {} for sku {} (version {})", event.type(), event.aggregateId(), p.version());
        indexer.index(new ProductDocument(p.sku(), p.nameEn(), p.nameAr(), p.brand(), p.category(),
            p.status(), new BigDecimal(p.price()), p.version()));
    }
}
