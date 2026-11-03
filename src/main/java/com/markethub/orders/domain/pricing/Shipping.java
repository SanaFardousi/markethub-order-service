package com.markethub.orders.domain.pricing;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Shipping {           // the shipping charge chosen by R4
    private final BigDecimal amount;
    public Shipping(BigDecimal amount) { this.amount = amount; }
}
