package com.markethub.orders.domain.state;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;

final class OnHold implements OrderState {
    public OrderStatus status() { return OrderStatus.ON_HOLD; }
    public OrderState approve(Order order) { return new Confirmed(); }
    public OrderState cancel(Order order)  { return new Cancelled(); }
}
