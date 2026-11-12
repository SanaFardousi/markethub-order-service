package com.markethub.orders.infrastructure.outbox;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry registry, OutboxRepository repository) {
        Gauge.builder("markethub.outbox.unpublished", repository, OutboxRepository::countByPublishedAtIsNull)
            .description("Outbox rows that Kafka has not acknowledged yet")
            .register(registry);
    }
}
