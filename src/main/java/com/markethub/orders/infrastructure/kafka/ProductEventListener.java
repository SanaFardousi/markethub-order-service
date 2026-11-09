package com.markethub.orders.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class ProductEventListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @KafkaListener(topics = Topics.PRODUCTS)
    public void onProductEvent(String json) {
        ProductEvent event = JSON.readValue(json, ProductEvent.class);
        log.info("Received {} for sku {} (version {})",
            event.type(), event.aggregateId(), event.payload().version());
    }
}
