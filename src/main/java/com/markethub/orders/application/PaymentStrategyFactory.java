package com.markethub.orders.application;

import com.markethub.orders.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {
    private final Map<PaymentMethod, PaymentStrategy> byMethod;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.byMethod = strategies.stream()
            .collect(Collectors.toUnmodifiableMap(PaymentStrategy::method, Function.identity()));
    }

    public PaymentStrategy forMethod(PaymentMethod method) {
        PaymentStrategy strategy = byMethod.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("No payment strategy for " + method);
        }
        return strategy;
    }
}
