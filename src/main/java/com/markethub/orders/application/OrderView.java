package com.markethub.orders.application;

import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.payment.PaymentStatus;
import com.markethub.orders.domain.pricing.AppliedPromotion;

import java.math.BigDecimal;
import java.util.List;

public record OrderView(String orderNumber, OrderStatus status, BigDecimal subtotal, BigDecimal discount,
                        BigDecimal shipping, BigDecimal total, List<AppliedPromotion> promotions,
                        List<LineView> lines, PaymentView payment) {
    public record LineView(String sku, String name, int quantity, BigDecimal unitPrice) {}
    public record PaymentView(PaymentMethod method, PaymentStatus status, String cardLast4) {}
}
