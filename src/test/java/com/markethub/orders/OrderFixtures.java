package com.markethub.orders;

import com.markethub.orders.domain.model.*;
import com.markethub.orders.domain.payment.CardPayment;
import com.markethub.orders.domain.payment.CodPayment;
import com.markethub.orders.domain.pricing.PricingResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderFixtures {

    public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private OrderFixtures() {}

    public static Builder anOrder() { return new Builder(); }

    public static final class Builder {
        private String orderNumber = "MH-2026-000001";
        private String idempotencyKey = UUID.randomUUID().toString();
        private String requestHash = "a".repeat(64);
        private OrderStatus status = OrderStatus.CONFIRMED;
        private int lineCount = 1;
        private Customer customer = new Customer("buyer@example.com", CustomerTier.SILVER, "KW");
        private Instant createdAt = NOW;
        private boolean cashOnDelivery = false;
        private BigDecimal subtotal = new BigDecimal("0.000");
        private BigDecimal discount = new BigDecimal("0.000");
        private BigDecimal shipping = new BigDecimal("0.000");

        public Builder withOrderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public Builder withIdempotencyKey(String key) { this.idempotencyKey = key; return this; }
        public Builder withRequestHash(String hash) { this.requestHash = hash; return this; }
        public Builder withStatus(OrderStatus status) { this.status = status; return this; }
        public Builder withLineCount(int lineCount) { this.lineCount = lineCount; return this; }
        public Builder withCustomer(Customer customer) { this.customer = customer; return this; }
        public Builder withCreatedAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder withCashOnDelivery() { this.cashOnDelivery = true; return this; }
        public Builder withTotals(String subtotal, String discount, String shipping) {
            this.subtotal = new BigDecimal(subtotal);
            this.discount = new BigDecimal(discount);
            this.shipping = new BigDecimal(shipping);
            return this;
        }

        public Order build() {
            Order order = new Order(orderNumber, customer, idempotencyKey, requestHash, null,
                new Address("KW", "Kuwait City", "Block 3, Street 12"), status, createdAt);
            for (int i = 1; i <= lineCount; i++) {
                order.addLine(new OrderLine("SKU-" + i, "Line " + i, 1, new BigDecimal("2.500")));
            }
            order.applyPricing(new PricingResult(subtotal, discount, shipping,
                subtotal.subtract(discount).add(shipping), List.of(), false));
            order.attachPayment(cashOnDelivery
                ? new CodPayment(order, order.getTotal())
                : new CardPayment(order, order.getTotal(), "1111", "AUTH-1"));
            return order;
        }
    }
}
