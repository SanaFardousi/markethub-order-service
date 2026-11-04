package com.markethub.orders.web.dto;

import com.markethub.orders.domain.model.Money;
import com.markethub.orders.domain.pricing.PricingResult;

import java.util.List;

public record PricingResponse(String currency, String subtotal, String discount, String shipping,
                              String total, List<PromotionDto> promotions, boolean onHold) {

    public record PromotionDto(String id, String description, String amount) {}

    public static PricingResponse from(PricingResult r) {
        return new PricingResponse(Money.CURRENCY,
            r.subtotal().toPlainString(), r.discount().toPlainString(),
            r.shipping().toPlainString(), r.total().toPlainString(),
            r.promotions().stream()
                .map(p -> new PromotionDto(p.id(), p.description(), p.amount().toPlainString()))
                .toList(),
            r.onHold());
    }
}
