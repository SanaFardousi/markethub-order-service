package com.markethub.orders.domain.pricing;

import com.markethub.orders.domain.model.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                            BigDecimal total, List<AppliedPromotion> promotions, boolean onHold) {

    public static PricingResult from(Basket basket, Collection<?> facts) {
        List<AppliedPromotion> promotions = new ArrayList<>();
        BigDecimal discount = Money.ZERO.amount();
        BigDecimal shipping = Money.ZERO.amount();
        boolean onHold = false;

        for (Object fact : facts) {
            if (fact instanceof Candidate c) {
                promotions.add(new AppliedPromotion(c.getId(), c.getDescription(), c.getAmount()));
            } else if (fact instanceof Discount d) {
                discount = d.getAmount();
            } else if (fact instanceof Shipping s) {
                shipping = s.getAmount();
            } else if (fact instanceof FraudHold) {
                onHold = true;
            }
        }
        BigDecimal subtotal = basket.getSubtotal();
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        return new PricingResult(subtotal, discount, shipping, total, List.copyOf(promotions), onHold);
    }

    public BigDecimal discountFor(String ruleId) {
        return promotions.stream()
            .filter(p -> p.id().equals(ruleId))
            .map(AppliedPromotion::amount)
            .reduce(Money.ZERO.amount(), BigDecimal::add);
    }
}
