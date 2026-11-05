package com.markethub.orders.domain.state;

import com.markethub.orders.domain.model.OrderStatus;

final class Cancelled implements OrderState {
    public OrderStatus status() { return OrderStatus.CANCELLED; }
}

