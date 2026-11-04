package com.markethub.orders.domain.pricing;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class Discount {           // the final discount after R3
    private final BigDecimal amount;
    public Discount(BigDecimal amount) { this.amount = amount; }
}
