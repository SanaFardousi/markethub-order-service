package com.markethub.orders.web.dto;

import java.util.List;

public record OrderResponse(String orderNumber, String status, String currency, String subtotal, String discount,
                            String shipping, String total, List<PromotionResponse> promotions,
                            List<LineResponse> lines, PaymentResponse payment) {
    public record PromotionResponse(String id, String description, String amount) {}
    public record LineResponse(String sku, String name, int quantity, String unitPrice) {}
    public record PaymentResponse(String method, String status, String cardLast4) {}
}

