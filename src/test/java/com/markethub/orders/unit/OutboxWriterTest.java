package com.markethub.orders.unit;

import com.jayway.jsonpath.JsonPath;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.infrastructure.kafka.Topics;
import com.markethub.orders.infrastructure.outbox.OutboxEvent;
import com.markethub.orders.infrastructure.outbox.OutboxWriter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.markethub.orders.OrderFixtures.NOW;
import static com.markethub.orders.OrderFixtures.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock EntityManager entityManager;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OutboxWriter writer;

    @BeforeEach
    void setUp() { writer = new OutboxWriter(entityManager, clock); }

    private List<OutboxEvent> written() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(entityManager, atLeastOnce()).persist(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void orderPlaced_shouldWriteTheEnvelopeKeyedByOrderNumber() {
        Order order = anOrder().withOrderNumber("MH-2026-000123").withTotals("33.000", "3.300", "0.000").build();

        writer.orderPlaced(order);

        OutboxEvent event = written().get(0);
        String json = event.getPayload();
        assertAll(
            () -> assertThat(event.getTopic()).isEqualTo(Topics.ORDERS),
            () -> assertThat(event.getAggregateId()).isEqualTo("MH-2026-000123"),
            () -> assertThat(event.getEventType()).isEqualTo("OrderPlaced"),
            () -> assertThat(event.getPublishedAt()).isNull(),
            () -> assertThat(JsonPath.<String>read(json, "$.eventId")).isEqualTo(event.getId()),
            () -> assertThat(JsonPath.<String>read(json, "$.type")).isEqualTo("OrderPlaced"),
            () -> assertThat(JsonPath.<String>read(json, "$.aggregateId")).isEqualTo("MH-2026-000123"),
            () -> assertThat(JsonPath.<String>read(json, "$.payload.total")).isEqualTo("29.700"));
    }

    @ParameterizedTest(name = "{0} writes {1}")
    @CsvSource({ "CONFIRMED, OrderConfirmed", "SHIPPED, OrderShipped",
        "DELIVERED, OrderDelivered", "CANCELLED, OrderCancelled" })
    void orderStatusChanged_shouldWriteTheEventForTheNewStatus(OrderStatus status, String expectedType) {
        writer.orderStatusChanged(anOrder().withStatus(status).build(), null);

        assertThat(written().get(0).getEventType()).isEqualTo(expectedType);
    }

    @Test
    void orderStatusChanged_shouldWriteNothing_whenOrderIsOnHold() {
        writer.orderStatusChanged(anOrder().withStatus(OrderStatus.ON_HOLD).build(), null);

        verifyNoInteractions(entityManager);
    }

    @Test
    void orderStatusChanged_shouldCarryTheReason_onlyWhenOneIsGiven() {
        Order cancelled = anOrder().withStatus(OrderStatus.CANCELLED).build();

        writer.orderStatusChanged(cancelled, "changed my mind");
        writer.orderStatusChanged(cancelled, null);

        List<OutboxEvent> events = written();
        assertAll(
            () -> assertThat(JsonPath.<String>read(events.get(0).getPayload(), "$.payload.reason"))
                .isEqualTo("changed my mind"),
            () -> assertThat(events.get(1).getPayload()).doesNotContain("reason"));
    }

    @Test
    @DisplayName("created_at never repeats, even when the clock does not move")
    void write_shouldGiveEveryEventALaterCreatedAt_evenWithAFrozenClock() {
        Order order = anOrder().build();

        writer.orderPlaced(order);
        writer.orderStatusChanged(order, null);

        List<OutboxEvent> events = written();
        assertThat(events.get(1).getCreatedAt()).isAfter(events.get(0).getCreatedAt());
    }
}
