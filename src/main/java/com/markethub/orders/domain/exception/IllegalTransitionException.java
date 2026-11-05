package com.markethub.orders.domain.exception;

import com.markethub.orders.domain.model.OrderStatus;

public class IllegalTransitionException extends ConflictException {
    public IllegalTransitionException(OrderStatus status, String action) {
        super("ILLEGAL_TRANSITION", "Cannot " + action + " an order in status " + status);
    }
}
