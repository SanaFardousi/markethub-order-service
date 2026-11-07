package com.markethub.orders.domain.state;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;

final class Confirmed implements OrderState {
    public OrderStatus status() { return OrderStatus.CONFIRMED; }
    public OrderState ship(Order order)   { return new Shipped(); }
    public OrderState cancel(Order order) { order.requestRefund(); return new Cancelled(); }
}
