package com.markethub.orders.infrastructure.outbox;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${markethub.outbox.poll-interval}")
    @Transactional
    public void relay() {
        for (OutboxEvent event : repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Could not publish outbox event {}; it will be retried on the next run", event.getId(), e);
                return;
            }
            event.markPublished(clock.instant());
        }
    }
}
