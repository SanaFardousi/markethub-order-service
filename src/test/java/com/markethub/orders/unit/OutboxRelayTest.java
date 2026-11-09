package com.markethub.orders.unit;

import com.markethub.orders.infrastructure.kafka.Topics;
import com.markethub.orders.infrastructure.outbox.OutboxEvent;
import com.markethub.orders.infrastructure.outbox.OutboxRelay;
import com.markethub.orders.infrastructure.outbox.OutboxRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.markethub.orders.OrderFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxRepository repository;
    @Mock
    KafkaTemplate<String, String> kafka;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OutboxRelay relay;

    @BeforeEach
    void setUp() { relay = new OutboxRelay(repository, kafka, clock); }

    private static OutboxEvent event(String id, String key) {
        return new OutboxEvent(id, key, "OrderPlaced", Topics.ORDERS, "{}", NOW);
    }

    private static CompletableFuture<SendResult<String, String>> acknowledged() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> failed() {
        return CompletableFuture.failedFuture(new IllegalStateException("broker down"));
    }

    @Test
    void relay_shouldSendEventsInOrderWithTheirKeyAndMarkThemPublished() {
        OutboxEvent first = event("e1", "MH-1");
        OutboxEvent second = event("e2", "MH-2");
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(acknowledged());

        relay.relay();

        InOrder inOrder = inOrder(kafka);
        inOrder.verify(kafka).send(Topics.ORDERS, "MH-1", "{}");
        inOrder.verify(kafka).send(Topics.ORDERS, "MH-2", "{}");
        assertAll(
            () -> assertThat(first.getPublishedAt()).isEqualTo(NOW),
            () -> assertThat(second.getPublishedAt()).isEqualTo(NOW));
    }

    @Test
    void relay_shouldLeaveEventUnpublishedAndSendNothingElse_whenTheFirstSendFails() {
        OutboxEvent first = event("e1", "MH-1");
        OutboxEvent second = event("e2", "MH-2");
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(failed());

        relay.relay();

        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertAll(
            () -> assertThat(first.getPublishedAt()).isNull(),
            () -> assertThat(second.getPublishedAt()).isNull());
    }

    @Test
    void relay_shouldKeepEarlierProgress_whenALaterSendFails() {
        OutboxEvent first = event("e1", "MH-1");
        OutboxEvent second = event("e2", "MH-2");
        when(repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafka.send(Topics.ORDERS, "MH-1", "{}")).thenReturn(acknowledged());
        when(kafka.send(Topics.ORDERS, "MH-2", "{}")).thenReturn(failed());

        relay.relay();

        assertAll(
            () -> assertThat(first.getPublishedAt()).isEqualTo(NOW),
            () -> assertThat(second.getPublishedAt()).isNull());
    }
}
