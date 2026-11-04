package com.markethub.orders.web.dto;

import com.markethub.orders.domain.payment.PaymentMethod;

import java.util.List;

public record PricingRequest(Long customerId, String couponCode, PaymentMethod paymentMethod,
                             List<LineRequest> lines) {
    public record LineRequest(String sku, int quantity) {}
}
