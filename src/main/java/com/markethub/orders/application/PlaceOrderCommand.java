package com.markethub.orders.application;

import com.markethub.orders.domain.model.Address;
import com.markethub.orders.domain.payment.PaymentMethod;

import java.util.List;
import java.util.stream.Collectors;

public record PlaceOrderCommand(Long customerId, String couponCode, PaymentMethod paymentMethod,
                                CardDetails card, Address shippingAddress, List<RequestedLine> lines,
                                String idempotencyKey) {
    /** A stable text form of the request body, used for the idempotency hash. */
    public String fingerprint() {
        String cardPart = card == null ? "" : card.last4() + "/" + card.expiry();
        String linePart = lines.stream().map(l -> l.sku() + "x" + l.quantity()).collect(Collectors.joining(","));
        return String.join("|", String.valueOf(customerId), String.valueOf(couponCode),
            String.valueOf(paymentMethod), cardPart, shippingAddress.country(), shippingAddress.city(),
            shippingAddress.line1(), linePart);
    }
}
