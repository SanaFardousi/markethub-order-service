package com.markethub.orders.domain.state;

import com.markethub.orders.domain.exception.IllegalTransitionException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;

public sealed interface OrderState permits OnHold, Confirmed, Shipped, Delivered, Cancelled {
    OrderStatus status();

    default OrderState approve(Order order) { throw illegal("APPROVE"); }
    default OrderState ship(Order order)    { throw illegal("SHIP"); }
    default OrderState deliver(Order order) { throw illegal("DELIVER"); }
    default OrderState cancel(Order order)  { throw illegal("CANCEL"); }

    private IllegalTransitionException illegal(String action) {
        return new IllegalTransitionException(status(), action);
    }

    static OrderState of(OrderStatus status) {
        return switch (status) {
            case ON_HOLD   -> new OnHold();
            case CONFIRMED -> new Confirmed();
            case SHIPPED   -> new Shipped();
            case DELIVERED -> new Delivered();
            case CANCELLED -> new Cancelled();
        };
    }
}
