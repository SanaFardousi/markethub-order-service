package com.markethub.orders.domain.state;

import com.markethub.orders.domain.model.OrderStatus;

final class Delivered implements OrderState {
    public OrderStatus status() { return OrderStatus.DELIVERED; }
}
