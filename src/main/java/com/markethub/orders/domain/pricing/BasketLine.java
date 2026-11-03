package com.markethub.orders.domain.pricing;

import com.markethub.orders.domain.model.Money;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BasketLine {
    private final String sku;
    private final int quantity;
    private final BigDecimal unitPrice;

    public BasketLine(String sku, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = new Money(unitPrice).amount();
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
