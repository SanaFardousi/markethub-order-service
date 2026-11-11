package com.markethub.orders.web.dto;

import com.markethub.orders.application.ShippingOption;
import com.markethub.orders.domain.model.Money;

public record ShippingOptionResponse(String carrier, String price, String currency, int estimatedDays) {
    public static ShippingOptionResponse from(ShippingOption option) {
        return new ShippingOptionResponse(option.carrier(), new Money(option.price()).amount().toPlainString(),
            Money.CURRENCY, option.estimatedDays());
    }
}
