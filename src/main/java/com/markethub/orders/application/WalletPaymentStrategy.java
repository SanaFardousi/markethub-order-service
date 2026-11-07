package com.markethub.orders.application;

import com.markethub.orders.domain.exception.PaymentDeclinedException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.Payment;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.payment.WalletPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletPaymentStrategy implements PaymentStrategy {
    private final PaymentGateway gateway;

    public PaymentMethod method() { return PaymentMethod.WALLET; }

    public Payment authorise(Order order, CardDetails card) {
        GatewayResult result = gateway.chargeWallet(order.getCustomer().getId(), order.getTotal());
        if (!result.approved()) {
            throw new PaymentDeclinedException("The wallet payment was declined");
        }
        return new WalletPayment(order, order.getTotal(), result.reference());
    }

    public void refund(Payment payment) { gateway.refund(payment.reference(), payment.getAmount()); }
}
