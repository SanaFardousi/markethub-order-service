package com.markethub.orders.domain.pricing;

import java.math.BigDecimal;

public record AppliedPromotion(String id, String description, BigDecimal amount) {}
