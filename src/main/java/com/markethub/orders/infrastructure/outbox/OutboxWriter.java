package com.markethub.orders.infrastructure.outbox;

import com.markethub.orders.domain.model.Money;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.model.Product;
import com.markethub.orders.infrastructure.kafka.ProductEvent;
import com.markethub.orders.infrastructure.kafka.Topics;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxWriter {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** The event a status change produces. ON_HOLD is deliberately missing: it has no event of its own. */
    private static final Map<OrderStatus, String> EVENT_FOR_STATUS = Map.of(
        OrderStatus.CONFIRMED, "OrderConfirmed",
        OrderStatus.SHIPPED, "OrderShipped",
        OrderStatus.DELIVERED, "OrderDelivered",
        OrderStatus.CANCELLED, "OrderCancelled");

    private final EntityManager entityManager;
    private final Clock clock;
    private final AtomicLong lastMicros = new AtomicLong();

    public OutboxWriter(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void orderPlaced(Order order) {
        write(Topics.ORDERS, order.getOrderNumber(), "OrderPlaced", orderPayload(order, null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void orderStatusChanged(Order order, String reason) {
        String type = EVENT_FOR_STATUS.get(order.getStatus());
        if (type != null) {
            write(Topics.ORDERS, order.getOrderNumber(), type, orderPayload(order, reason));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void productUpserted(Product product) {
        ProductEvent.Payload payload = new ProductEvent.Payload(product.getSku(), product.getNameEn(),
            product.getNameAr(), product.getBrand(), product.getCategory(),
            product.getPriceAmount().toPlainString(), product.getCurrency(),
            product.getStatus().name(), product.getVersion());
        write(Topics.PRODUCTS, product.getSku(), "ProductUpserted", payload);
    }

    private void write(String topic, String aggregateId, String type, Object payload) {
        Instant createdAt = nextCreatedAt();
        String eventId = UUID.randomUUID().toString();
        EventEnvelope envelope = new EventEnvelope(eventId, type,
            createdAt.truncatedTo(ChronoUnit.MILLIS).toString(), aggregateId, payload);
        entityManager.persist(new OutboxEvent(eventId, aggregateId, type, topic,
            JSON.writeValueAsString(envelope), createdAt));
    }

    /** created_at is the only column the relay can sort by, so it must never repeat. */
    private Instant nextCreatedAt() {
        long nowMicros = ChronoUnit.MICROS.between(Instant.EPOCH, clock.instant());
        long micros = lastMicros.updateAndGet(last -> Math.max(nowMicros, last + 1));
        return Instant.EPOCH.plus(micros, ChronoUnit.MICROS);
    }

    private static OrderEventPayload orderPayload(Order order, String reason) {
        List<OrderEventPayload.Line> lines = order.getLines().stream()
            .map(l -> new OrderEventPayload.Line(l.getSku(), l.getName(), l.getQuantity(),
                l.getUnitPrice().toPlainString()))
            .toList();
        return new OrderEventPayload(order.getOrderNumber(), order.getCustomer().getId(),
            order.getStatus().name(), order.getPayment().method().name(), Money.CURRENCY,
            order.getSubtotal().toPlainString(), order.getDiscount().toPlainString(),
            order.getShipping().toPlainString(), order.getTotal().toPlainString(), lines, reason);
    }
}
