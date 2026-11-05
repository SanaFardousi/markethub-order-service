package com.markethub.orders.domain.state;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;

final class Shipped implements OrderState {
    public OrderStatus status() { return OrderStatus.SHIPPED; }
    public OrderState deliver(Order order) { return new Delivered(); }
}
