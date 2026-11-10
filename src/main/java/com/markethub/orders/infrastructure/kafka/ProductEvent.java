package com.markethub.orders.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)      // ignore fields we don't know about, so adding one later won't break readers
public record ProductEvent(String eventId, String type, String occurredAt,
                           String aggregateId, Payload payload) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(String sku, String nameEn, String nameAr, String brand, String category,
                          String price, String currency, String status, Long version) {}
}
