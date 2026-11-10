package com.markethub.orders.infrastructure.outbox;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)       // leave out fields that are null
public record OrderEventPayload(String orderNumber, Long customerId, String status, String paymentMethod,
                                String currency, String subtotal, String discount, String shipping,
                                String total, List<Line> lines, String reason) {
    public record Line(String sku, String name, int quantity, String unitPrice) {}
}
