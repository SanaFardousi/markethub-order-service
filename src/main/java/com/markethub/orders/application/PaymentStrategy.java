package com.markethub.orders.application;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.Payment;
import com.markethub.orders.domain.payment.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod method();
    Payment authorise(Order order, CardDetails card);   // throws PaymentDeclinedException
    void refund(Payment payment);
}
