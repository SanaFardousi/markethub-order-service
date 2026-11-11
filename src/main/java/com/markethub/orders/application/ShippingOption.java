package com.markethub.orders.application;

import java.math.BigDecimal;

public record ShippingOption(String carrier, BigDecimal price, int estimatedDays) {}

