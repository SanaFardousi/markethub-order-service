package com.markethub.orders.persistence;

import com.markethub.orders.infrastructure.kafka.Topics;
import com.markethub.orders.infrastructure.outbox.OutboxEvent;
import com.markethub.orders.infrastructure.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.IntStream;

import static com.markethub.orders.persistence.PersistenceFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.UUID;

class OutboxRepositoryTest extends AbstractPersistenceTest {

    @Autowired
    OutboxRepository repository;

    private static OutboxEvent event(Instant createdAt) {
        return new OutboxEvent(UUID.randomUUID().toString(), "MH-1", "OrderPlaced", Topics.ORDERS, "{\"n\": 1}", createdAt);
    }

    @Test
    void findTop50_shouldReturnOnlyUnpublishedEventsOldestFirst() {
        OutboxEvent newest = event(NOW.plusSeconds(20));
        OutboxEvent oldest = event(NOW);
        OutboxEvent alreadySent = event(NOW.minusSeconds(5));
        alreadySent.markPublished(NOW);
        repository.saveAllAndFlush(List.of(newest, alreadySent, oldest));

        List<OutboxEvent> batch = repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertThat(batch).extracting(OutboxEvent::getId).containsExactly(oldest.getId(), newest.getId());
    }

    @Test
    void findTop50_shouldReturnAtMostFiftyEvents() {
        List<OutboxEvent> many = IntStream.range(0, 55).mapToObj(i -> event(NOW.plusSeconds(i))).toList();
        repository.saveAllAndFlush(many);

        assertThat(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).hasSize(50);
    }
}
