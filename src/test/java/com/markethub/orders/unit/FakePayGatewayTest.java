package com.markethub.orders.unit;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.markethub.orders.application.CardDetails;
import com.markethub.orders.application.GatewayResult;
import com.markethub.orders.domain.exception.PaymentTimeoutException;
import com.markethub.orders.infrastructure.provider.fakepay.FakePayGateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class FakePayGatewayTest {

    private final FakePayGateway gateway = new FakePayGateway();

    private static CardDetails cardEndingIn(String last4) {
        return new CardDetails("41111111111" + last4 + "1".repeat(1), "12/28");
    }

    @Test
    void authoriseCard_shouldApproveAnOrdinaryCard() {
        GatewayResult result = gateway.authoriseCard(new CardDetails("4111111111111111", "12/28"), new BigDecimal("29.700"));

        assertAll(
            () -> assertThat(result.approved()).isTrue(),
            () -> assertThat(result.reference()).startsWith("FP-"));
    }

    @Test
    void authoriseCard_shouldDecline_whenTheCardEndsIn0002() {
        GatewayResult result = gateway.authoriseCard(new CardDetails("4111111111110002", "12/28"), new BigDecimal("29.700"));

        assertThat(result.approved()).isFalse();
    }

    @Test
    void authoriseCard_shouldThrowOurTimeout_whenTheVendorTimesOut() {
        assertThatThrownBy(() -> gateway.authoriseCard(new CardDetails("4111111111110003", "12/28"), new BigDecimal("29.700")))
            .isInstanceOf(PaymentTimeoutException.class);
    }

    @Test
    void chargeWallet_shouldApproveUpToTheLimitAndDeclineAboveIt() {
        assertAll(
            () -> assertThat(gateway.chargeWallet(1L, new BigDecimal("1000.000")).approved()).isTrue(),
            () -> assertThat(gateway.chargeWallet(1L, new BigDecimal("1000.001")).approved()).isFalse());
    }

    @Test
    void refund_shouldSucceedWithoutAnError() {
        assertThatCode(() -> gateway.refund("FP-1", new BigDecimal("29.700"))).doesNotThrowAnyException();
    }
}
