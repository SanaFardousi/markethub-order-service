package com.markethub.orders.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {
    public static final String CURRENCY = "KWD";
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money{
        amount = amount.setScale(3, RoundingMode.HALF_UP);
    }

    public Money plus(Money other) { return new Money(amount.add(other.amount));}
    public Money minus(Money other) { return  new Money(amount.subtract(other.amount));}
    public boolean isNegative() { return amount.signum() < 0; }
}
