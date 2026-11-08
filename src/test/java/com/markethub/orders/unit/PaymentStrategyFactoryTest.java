package com.markethub.orders.unit;

import com.markethub.orders.application.PaymentStrategy;
import com.markethub.orders.application.PaymentStrategyFactory;
import com.markethub.orders.domain.payment.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentStrategyFactoryTest {

    private static PaymentStrategy strategyFor(PaymentMethod method) {
        PaymentStrategy strategy = mock(PaymentStrategy.class);
        when(strategy.method()).thenReturn(method);
        return strategy;
    }

    @Test
    void forMethod_shouldReturnTheStrategyRegisteredForThatMethod() {
        PaymentStrategy card = strategyFor(PaymentMethod.CARD);
        PaymentStrategy cod = strategyFor(PaymentMethod.COD);

        PaymentStrategyFactory factory = new PaymentStrategyFactory(List.of(card, cod));

        assertAll(
            () -> assertThat(factory.forMethod(PaymentMethod.CARD)).isSameAs(card),
            () -> assertThat(factory.forMethod(PaymentMethod.COD)).isSameAs(cod));
    }

    @Test
    void forMethod_shouldThrow_whenNoStrategyIsRegisteredForThatMethod() {
        PaymentStrategyFactory factory = new PaymentStrategyFactory(List.of(strategyFor(PaymentMethod.CARD)));

        assertThatThrownBy(() -> factory.forMethod(PaymentMethod.WALLET))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldFail_whenTwoStrategiesClaimTheSameMethod() {
        List<PaymentStrategy> clashing = List.of(strategyFor(PaymentMethod.CARD), strategyFor(PaymentMethod.CARD));

        assertThatThrownBy(() -> new PaymentStrategyFactory(clashing)).isInstanceOf(IllegalStateException.class);
    }
}
