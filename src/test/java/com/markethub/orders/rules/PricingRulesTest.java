package com.markethub.orders.rules;

import com.markethub.orders.domain.model.CustomerTier;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.pricing.*;
import com.markethub.orders.infrastructure.drools.DroolsPricingEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kie.api.KieServices;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;


class PricingRulesTest {

    private static DroolsPricingEngine engine;

    @BeforeAll
    static void buildEngine() {
        engine = new DroolsPricingEngine(KieServices.Factory.get().getKieClasspathContainer());
    }

    // ---- the worked example (section 4.1) ----
    @Test
    void price_shouldReturn29_700_forTheWorkedExample() {
        Basket basket = new Basket(List.of(
            new BasketLine("MH-OUD-50", 2, new BigDecimal("12.500")),
            new BasketLine("MH-ROSE-30", 1, new BigDecimal("8.000"))), "WELCOME10", PaymentMethod.CARD);

        PricingResult result = engine.price(basket, customer(CustomerTier.SILVER, 0));

        assertThat(result.subtotal()).isEqualByComparingTo("33.000");
        assertThat(result.discount()).isEqualByComparingTo("3.300");
        assertThat(result.shipping()).isEqualByComparingTo("0.000");
        assertThat(result.total()).isEqualByComparingTo("29.700");
        assertThat(result.promotions()).extracting(AppliedPromotion::id).containsExactly("R2");
    }

    // ---- R1: basket tier ----
    @ParameterizedTest(name = "subtotal {0} -> R1 discount {1}")
    @CsvSource({ "10.000, 0.000", "19.999, 0.000", "20.000, 1.000", "30.000, 1.500",
        "49.999, 2.500", "50.000, 5.000", "80.000, 8.000" })
    void basketTier_shouldApplyHighestTierOnly(BigDecimal subtotal, BigDecimal expected) {
        PricingResult result = engine.price(
            basketWithSubtotal(subtotal, null, PaymentMethod.CARD), customer(CustomerTier.SILVER, 3));

        assertThat(result.discountFor("R1")).isEqualByComparingTo(expected);
    }

    // ---- R2: welcome coupon ----
    @ParameterizedTest(name = "{0} earlier orders, coupon {1}, subtotal {2} -> discount {3}")
    @CsvSource(nullValues = "NONE", value = {
        "0, WELCOME10, 10.000, 1.000",    // applies
        "1, WELCOME10, 10.000, 0.000",    // one earlier order: not eligible
        "0, NONE,      10.000, 0.000",    // no coupon
        "0, SUMMER5,   10.000, 0.000",    // some other coupon
        "0, WELCOME10, 49.999, 5.000",    // 4.9999 rounds up to the 5.000 cap
        "0, WELCOME10, 50.000, 5.000" })  // cap boundary
    void welcomeCoupon_shouldGiveTenPercentCappedAtFive_whenNoEarlierOrders(
        long earlierOrders, String coupon, BigDecimal subtotal, BigDecimal expected) {
        PricingResult result = engine.price(
            basketWithSubtotal(subtotal, coupon, PaymentMethod.CARD), customer(CustomerTier.SILVER, earlierOrders));

        assertThat(result.discount()).isEqualByComparingTo(expected);
    }

    // ---- R3: no stacking ----
    @ParameterizedTest(name = "subtotal {0} -> discount {1}, winner {2}")
    @CsvSource({ "33.000, 3.300, R2", "80.000, 8.000, R1" })
    void noStacking_shouldKeepTheLargerOfR1AndR2(BigDecimal subtotal, BigDecimal expectedDiscount, String winner) {
        PricingResult result = engine.price(
            basketWithSubtotal(subtotal, "WELCOME10", PaymentMethod.CARD), customer(CustomerTier.SILVER, 0));

        assertThat(result.discount()).isEqualByComparingTo(expectedDiscount);
        assertThat(result.promotions()).extracting(AppliedPromotion::id).containsExactly(winner);
    }

    @Test
    void noStacking_shouldKeepExactlyOnePromotion_whenR1AndR2AreEqual() {
        PricingResult result = engine.price(
            basketWithSubtotal(new BigDecimal("50.000"), "WELCOME10", PaymentMethod.CARD),
            customer(CustomerTier.SILVER, 0));

        assertThat(result.discount()).isEqualByComparingTo("5.000");
        assertThat(result.promotions()).hasSize(1);
    }

    // ---- R4: shipping ----
    @ParameterizedTest(name = "{0}, {1} earlier, coupon {2}, subtotal {3} -> shipping {4}")
    @CsvSource(nullValues = "NONE", value = {
        "SILVER, 3, NONE,      10.000, 1.500",   // flat fee
        "SILVER, 3, NONE,      14.999, 1.500",   // just below 15
        "SILVER, 3, NONE,      15.000, 0.000",   // boundary: free
        "SILVER, 3, NONE,      20.000, 0.000",   // discounted to 19.000, still free
        "SILVER, 0, WELCOME10, 16.666, 1.500",   // 16.666 - 1.667 = 14.999: not free
        "SILVER, 0, WELCOME10, 16.667, 0.000",   // 16.667 - 1.667 = 15.000: free
        "GOLD,   3, NONE,       5.000, 0.000" }) // GOLD is always free
    void shipping_shouldBeFree_forGoldOrWhenSubtotalMinusDiscountReaches15(
        CustomerTier tier, long earlierOrders, String coupon, BigDecimal subtotal, BigDecimal expected) {
        PricingResult result = engine.price(
            basketWithSubtotal(subtotal, coupon, PaymentMethod.CARD), customer(tier, earlierOrders));

        assertThat(result.shipping()).isEqualByComparingTo(expected);
    }

    // ---- F1: fraud hold ----
    @ParameterizedTest(name = "{0}, subtotal {1} -> on hold {2}")
    @CsvSource({ "COD, 50.000, false",       // small COD order
        "COD, 111.111, false",      // total exactly 100.000: not above
        "COD, 111.112, true",       // total 100.001: held
        "CARD, 500.000, false",     // not cash on delivery
        "WALLET, 500.000, false" })
    void fraudHold_shouldHoldOnlyCashOnDeliveryAbove100(PaymentMethod method, BigDecimal subtotal, boolean expected) {
        PricingResult result = engine.price(
            basketWithSubtotal(subtotal, null, method), customer(CustomerTier.SILVER, 3));

        assertThat(result.onHold()).isEqualTo(expected);
    }

    // ---- 50 concurrent requests: proves no KieSession is shared ----
    @Test
    void price_shouldGiveCorrectResults_when50ThreadsPriceAtOnce() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<PricingResult>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                boolean withCoupon = i % 2 == 0;
                futures.add(pool.submit(() -> {
                    start.await();
                    return withCoupon
                        ? engine.price(basketWithSubtotal(new BigDecimal("33.000"), "WELCOME10", PaymentMethod.CARD),
                        customer(CustomerTier.SILVER, 0))
                        : engine.price(basketWithSubtotal(new BigDecimal("10.000"), null, PaymentMethod.CARD),
                        customer(CustomerTier.SILVER, 3));
                }));
            }
            start.countDown();   // release all threads at the same moment

            for (int i = 0; i < threads; i++) {
                PricingResult result = futures.get(i).get(10, TimeUnit.SECONDS);
                assertThat(result.total()).isEqualByComparingTo(i % 2 == 0 ? "29.700" : "11.500");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- helpers ----
    private static Basket basketWithSubtotal(BigDecimal subtotal, String coupon, PaymentMethod method) {
        return new Basket(List.of(new BasketLine("TEST-SKU", 1, subtotal)), coupon, method);
    }

    private static CustomerFact customer(CustomerTier tier, long earlierOrders) {
        return new CustomerFact(tier, earlierOrders);
    }
}
