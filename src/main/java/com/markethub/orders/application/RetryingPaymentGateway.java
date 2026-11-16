package com.markethub.orders.application;

import com.markethub.orders.domain.exception.PaymentTimeoutException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class RetryingPaymentGateway implements PaymentGateway {

    private final PaymentGateway delegate;
    private final int maxAttempts;
    private final Duration backoff;
    private final Consumer<Duration> pause;

    public RetryingPaymentGateway(PaymentGateway delegate, int maxAttempts, Duration backoff) {
        this(delegate, maxAttempts, backoff, RetryingPaymentGateway::sleep);
    }

    public RetryingPaymentGateway(PaymentGateway delegate, int maxAttempts, Duration backoff,
                                  Consumer<Duration> pause) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
        this.pause = pause;
    }

    @Override
    public GatewayResult authoriseCard(CardDetails card, BigDecimal amount) {
        return withRetry("authoriseCard", () -> delegate.authoriseCard(card, amount));
    }

    @Override
    public GatewayResult chargeWallet(Long customerId, BigDecimal amount) {
        return withRetry("chargeWallet", () -> delegate.chargeWallet(customerId, amount));
    }

    @Override
    public void refund(String reference, BigDecimal amount) {
        withRetry("refund", () -> {
            delegate.refund(reference, amount);
            return null;
        });
    }

    /** Repeats the call, but only when it timed out. Every other outcome, including a decline, ends it at once. */
    private <T> T withRetry(String operation, Supplier<T> call) {
        PaymentTimeoutException lastTimeout = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (PaymentTimeoutException timeout) {
                lastTimeout = timeout;
                log.warn("{} timed out (attempt {} of {})", operation, attempt, maxAttempts);
                if (attempt < maxAttempts) {
                    pause.accept(backoff.multipliedBy(attempt));     // 100 ms, then 200 ms, ...
                }
            }
        }
        throw lastTimeout;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentTimeoutException("Interrupted while waiting to retry", e);
        }
    }
}
