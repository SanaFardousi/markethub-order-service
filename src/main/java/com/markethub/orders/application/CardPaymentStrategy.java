package com.markethub.orders.application;

import com.markethub.orders.domain.exception.BadRequestException;
import com.markethub.orders.domain.exception.PaymentDeclinedException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {
    private final PaymentGateway gateway;

    public PaymentMethod method() { return PaymentMethod.CARD; }

    public Payment authorise(Order order, CardDetails card) {
        if (card == null) {
            throw new BadRequestException("Card details are required for card payments");
        }
        GatewayResult result = gateway.authoriseCard(card, order.getTotal());
        if (!result.approved()) {
            throw new PaymentDeclinedException("The card was declined");
        }
        return new CardPayment(order, order.getTotal(), card.last4(), result.reference());
    }

    public void refund(Payment payment) { gateway.refund(payment.reference(), payment.getAmount()); }
}

