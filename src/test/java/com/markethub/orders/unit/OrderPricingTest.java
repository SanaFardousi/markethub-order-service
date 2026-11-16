package com.markethub.orders.unit;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.pricing.PricingResult;

import static com.markethub.orders.OrderFixtures.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderPricingTest {

    private static PricingResult pricing(String subtotal, String discount, String shipping) {
        BigDecimal s = new BigDecimal(subtotal);
        BigDecimal d = new BigDecimal(discount);
        BigDecimal sh = new BigDecimal(shipping);
        return new PricingResult(s, d, sh, s.subtract(d).add(sh), List.of(), false);
    }

    @Test
    void applyPricing_shouldSetTotalToSubtotalMinusDiscountPlusShipping() {
        Order order = anOrder().build();

        order.applyPricing(pricing("33.000", "3.300", "1.500"));

        assertAll(
            () -> assertThat(order.getSubtotal()).isEqualByComparingTo("33.000"),
            () -> assertThat(order.getDiscount()).isEqualByComparingTo("3.300"),
            () -> assertThat(order.getShipping()).isEqualByComparingTo("1.500"),
            () -> assertThat(order.getTotal()).isEqualByComparingTo("31.200"));
    }

    @Test
    void applyPricing_shouldRejectANegativeTotal_andLeaveTheOrderUnchanged() {
        Order order = anOrder().build();

        assertThatThrownBy(() -> order.applyPricing(pricing("10.000", "15.000", "0.000")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(order.getTotal()).isEqualByComparingTo("0.000");
    }
}
