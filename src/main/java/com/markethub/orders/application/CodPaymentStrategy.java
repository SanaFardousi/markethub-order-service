package com.markethub.orders.application;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.CodPayment;
import com.markethub.orders.domain.payment.Payment;
import com.markethub.orders.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class CodPaymentStrategy implements PaymentStrategy {
    public PaymentMethod method() { return PaymentMethod.COD; }
    public Payment authorise(Order order, CardDetails card) { return new CodPayment(order, order.getTotal()); }
    public void refund(Payment payment) { /* nothing was collected, so nothing to refund */ }
}
