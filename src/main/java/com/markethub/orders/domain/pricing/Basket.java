package com.markethub.orders.domain.pricing;

import com.markethub.orders.domain.model.Money;
import com.markethub.orders.domain.payment.PaymentMethod;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Getter
public class Basket {
    private final List<BasketLine> lines;
    private final String couponCode;
    private final PaymentMethod paymentMethod;

    public Basket(List<BasketLine> lines, String couponCode, PaymentMethod paymentMethod) {
        this.lines = List.copyOf(lines);
        this.couponCode = couponCode;
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getSubtotal() {
        BigDecimal sum = lines.stream().map(BasketLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Money(sum).amount();
    }

    public BigDecimal percentOf(int percent) {
        return getSubtotal().multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    }
}
