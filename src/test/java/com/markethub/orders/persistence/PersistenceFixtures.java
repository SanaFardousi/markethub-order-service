package com.markethub.orders.persistence;

import com.markethub.orders.domain.model.*;
import com.markethub.orders.domain.payment.CardPayment;

import java.math.BigDecimal;
import java.time.Instant;

final class PersistenceFixtures {
    static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    static Product product(String sku) {
        return new Product(sku, "Test " + sku, "اختبار " + sku, "MAISON", "fragrance",
            new BigDecimal("1.000"), ProductStatus.ACTIVE, NOW);
    }

    static Customer customer(String email) { return new Customer(email, CustomerTier.SILVER, "KW"); }

    static Order order(Customer customer, String number, String key, int lineCount) {
        Order order = new Order(number, customer, key, "a".repeat(64), null,
            new Address("KW", "Kuwait City", "Block 3, Street 12"), OrderStatus.CONFIRMED, NOW);
        for (int i = 1; i <= lineCount; i++) {
            order.addLine(new OrderLine("SKU-" + i, "Line " + i, 1, new BigDecimal("2.500")));
        }
        order.attachPayment(new CardPayment(order, order.getTotal(), "1111", "AUTH-1"));
        return order;
    }
}
