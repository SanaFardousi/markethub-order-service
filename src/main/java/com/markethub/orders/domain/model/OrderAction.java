package com.markethub.orders.domain.model;

import java.time.Instant;
import java.util.function.BiConsumer;

public enum OrderAction {
    APPROVE(Order::approve), SHIP(Order::ship), DELIVER(Order::deliver);

    private final BiConsumer<Order, Instant> operation;
    OrderAction(BiConsumer<Order, Instant> operation) { this.operation = operation; }

    public void applyTo(Order order, Instant now) { operation.accept(order, now); }
}

