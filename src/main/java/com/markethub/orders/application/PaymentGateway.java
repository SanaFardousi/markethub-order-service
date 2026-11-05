package com.markethub.orders.application;

import java.math.BigDecimal;

public interface PaymentGateway {
    GatewayResult authoriseCard(CardDetails card, BigDecimal amount);
    GatewayResult chargeWallet(Long customerId, BigDecimal amount);
    void refund(String reference, BigDecimal amount);
}
