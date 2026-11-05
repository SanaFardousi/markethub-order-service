package com.markethub.orders;

import com.markethub.orders.domain.model.*;
import com.markethub.orders.domain.payment.CardPayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderFixtures {

    public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private OrderFixtures() {}

    public static Builder anOrder() { return new Builder(); }

    public static final class Builder {
        private String orderNumber = "MH-2026-000001";
        private String idempotencyKey = UUID.randomUUID().toString();
        private OrderStatus status = OrderStatus.CONFIRMED;
        private int lineCount = 1;
        private Customer customer = new Customer("buyer@example.com", CustomerTier.SILVER, "KW");

        public Builder withOrderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public Builder withIdempotencyKey(String key) { this.idempotencyKey = key; return this; }
        public Builder withStatus(OrderStatus status) { this.status = status; return this; }
        public Builder withLineCount(int lineCount) { this.lineCount = lineCount; return this; }
        public Builder withCustomer(Customer customer) { this.customer = customer; return this; }

        public Order build() {
            Order order = new Order(orderNumber, customer, idempotencyKey, "a".repeat(64), null,
                new Address("KW", "Kuwait City", "Block 3, Street 12"), status, NOW);
            for (int i = 1; i <= lineCount; i++) {
                order.addLine(new OrderLine("SKU-" + i, "Line " + i, 1, new BigDecimal("2.500")));
            }
            order.attachPayment(new CardPayment(order, order.getTotal(), "1111", "AUTH-1"));
            return order;
        }
    }
}
