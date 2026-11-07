package com.markethub.orders.application;

import com.markethub.orders.domain.pricing.PricingResult;

import java.math.BigDecimal;
import java.util.List;

public record PricedBasket(PricingResult pricing, List<PricedLine> lines) {
    public record PricedLine(String sku, String name, int quantity, BigDecimal unitPrice) {}
}
