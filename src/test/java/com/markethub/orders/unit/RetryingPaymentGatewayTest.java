package com.markethub.orders.unit;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markethub.orders.application.CardDetails;
import com.markethub.orders.application.GatewayResult;
import com.markethub.orders.application.PaymentGateway;
import com.markethub.orders.application.RetryingPaymentGateway;
import com.markethub.orders.domain.exception.PaymentTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryingPaymentGatewayTest {

    private static final BigDecimal AMOUNT = new BigDecimal("29.700");

    @Mock PaymentGateway delegate;

    private final List<Duration> pauses = new ArrayList<>();
    private final CardDetails card = new CardDetails("4111111111111111", "12/28");
    private RetryingPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new RetryingPaymentGateway(delegate, 3, Duration.ofMillis(100), pauses::add);
    }

    @Test
    void authoriseCard_shouldCallTheProviderOnce_whenItAnswersStraightAway() {
        when(delegate.authoriseCard(card, AMOUNT)).thenReturn(new GatewayResult(true, "FP-1"));

        GatewayResult result = gateway.authoriseCard(card, AMOUNT);

        assertAll(
            () -> assertThat(result.approved()).isTrue(),
            () -> assertThat(pauses).isEmpty());
        verify(delegate, times(1)).authoriseCard(card, AMOUNT);
    }

    @Test
    void authoriseCard_shouldRetryAndSucceed_whenTheFirstCallTimesOut() {
        when(delegate.authoriseCard(card, AMOUNT))
            .thenThrow(new PaymentTimeoutException("no answer"))
            .thenReturn(new GatewayResult(true, "FP-2"));

        GatewayResult result = gateway.authoriseCard(card, AMOUNT);

        assertAll(
            () -> assertThat(result.approved()).isTrue(),
            () -> assertThat(pauses).containsExactly(Duration.ofMillis(100)));
        verify(delegate, times(2)).authoriseCard(card, AMOUNT);
    }

    @Test
    @DisplayName("it gives up after three attempts and waits a little longer each time")
    void authoriseCard_shouldRethrowTheTimeout_whenEveryAttemptTimesOut() {
        when(delegate.authoriseCard(card, AMOUNT)).thenThrow(new PaymentTimeoutException("no answer"));

        assertThatThrownBy(() -> gateway.authoriseCard(card, AMOUNT)).isInstanceOf(PaymentTimeoutException.class);

        assertThat(pauses).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200));
        verify(delegate, times(3)).authoriseCard(card, AMOUNT);
    }

    @Test
    @DisplayName("a decline is an answer, not a failure, so it is never retried")
    void authoriseCard_shouldNeverRetry_whenTheCardIsDeclined() {
        when(delegate.authoriseCard(card, AMOUNT)).thenReturn(new GatewayResult(false, "FP-3"));

        GatewayResult result = gateway.authoriseCard(card, AMOUNT);

        assertAll(
            () -> assertThat(result.approved()).isFalse(),
            () -> assertThat(pauses).isEmpty());
        verify(delegate, times(1)).authoriseCard(card, AMOUNT);
    }

    @Test
    void authoriseCard_shouldNotRetry_whenTheFailureIsNotATimeout() {
        when(delegate.authoriseCard(card, AMOUNT)).thenThrow(new IllegalStateException("bug"));

        assertThatThrownBy(() -> gateway.authoriseCard(card, AMOUNT)).isInstanceOf(IllegalStateException.class);

        verify(delegate, times(1)).authoriseCard(card, AMOUNT);
    }

    @Test
    void chargeWallet_shouldRetryOnTimeout() {
        when(delegate.chargeWallet(1L, AMOUNT))
            .thenThrow(new PaymentTimeoutException("no answer"))
            .thenReturn(new GatewayResult(true, "FP-4"));

        assertThat(gateway.chargeWallet(1L, AMOUNT).approved()).isTrue();

        verify(delegate, times(2)).chargeWallet(1L, AMOUNT);
    }

    @Test
    void refund_shouldRetryOnTimeout() {
        doThrow(new PaymentTimeoutException("no answer")).doNothing().when(delegate).refund("FP-1", AMOUNT);

        gateway.refund("FP-1", AMOUNT);

        verify(delegate, times(2)).refund("FP-1", AMOUNT);
    }

    @Test
    void constructor_shouldReject_whenNoAttemptsAreAllowed() {
        assertThatThrownBy(() -> new RetryingPaymentGateway(delegate, 0, Duration.ofMillis(100)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
