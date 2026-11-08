package com.markethub.orders.unit;

import com.markethub.orders.application.*;
import com.markethub.orders.domain.exception.BadRequestException;
import com.markethub.orders.domain.exception.PaymentDeclinedException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.payment.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.markethub.orders.OrderFixtures.anOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class PaymentStrategiesTest {

    @Mock
    PaymentGateway gateway;

    private final Order order = anOrder().withTotals("33.000", "3.300", "0.000").build();
    private final CardDetails card = new CardDetails("4111111111111111", "12/28");

    @Nested
    @DisplayName("card payments")
    class Card {
        @Test
        void authorise_shouldReturnCardPaymentWithLast4AndReference_whenGatewayApproves() {
            when(gateway.authoriseCard(any(CardDetails.class), eq(order.getTotal())))
                .thenReturn(new GatewayResult(true, "FP-1"));

            Payment payment = new CardPaymentStrategy(gateway).authorise(order, card);

            assertAll(
                () -> assertThat(payment).isInstanceOf(CardPayment.class),
                () -> assertThat(payment.cardLast4()).isEqualTo("1111"),
                () -> assertThat(payment.reference()).isEqualTo("FP-1"),
                () -> assertThat(payment.getAmount()).isEqualByComparingTo("29.700"),
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORISED));
        }

        @Test
        void authorise_shouldThrowDeclined_whenGatewayDeclines() {
            when(gateway.authoriseCard(any(CardDetails.class), any())).thenReturn(new GatewayResult(false, "FP-2"));

            assertThatThrownBy(() -> new CardPaymentStrategy(gateway).authorise(order, card))
                .isInstanceOf(PaymentDeclinedException.class);
        }

        @Test
        void authorise_shouldThrowBadRequest_whenCardIsMissing() {
            assertThatThrownBy(() -> new CardPaymentStrategy(gateway).authorise(order, null))
                .isInstanceOf(BadRequestException.class);
            verifyNoInteractions(gateway);
        }

        @Test
        void refund_shouldAskGatewayToRefundTheOriginalReference() {
            Payment payment = order.getPayment();

            new CardPaymentStrategy(gateway).refund(payment);

            verify(gateway).refund("AUTH-1", payment.getAmount());
        }
    }

    @Nested
    @DisplayName("wallet payments")
    class Wallet {
        @Test
        void authorise_shouldReturnWalletPayment_whenGatewayApproves() {
            when(gateway.chargeWallet(any(), eq(order.getTotal()))).thenReturn(new GatewayResult(true, "FP-3"));

            Payment payment = new WalletPaymentStrategy(gateway).authorise(order, null);

            assertAll(
                () -> assertThat(payment).isInstanceOf(WalletPayment.class),
                () -> assertThat(payment.reference()).isEqualTo("FP-3"));
        }

        @Test
        void authorise_shouldThrowDeclined_whenGatewayDeclines() {
            when(gateway.chargeWallet(any(), any())).thenReturn(new GatewayResult(false, "FP-4"));

            assertThatThrownBy(() -> new WalletPaymentStrategy(gateway).authorise(order, null))
                .isInstanceOf(PaymentDeclinedException.class);
        }
    }

    @Nested
    @DisplayName("cash on delivery")
    class CashOnDelivery {
        @Test
        void authorise_shouldRecordPendingPaymentWithoutCallingTheGateway() {
            Payment payment = new CodPaymentStrategy().authorise(order, null);

            assertAll(
                () -> assertThat(payment).isInstanceOf(CodPayment.class),
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING));
            verifyNoInteractions(gateway);
        }

        @Test
        void refund_shouldDoNothing_becauseNothingWasCollected() {
            new CodPaymentStrategy().refund(order.getPayment());

            verifyNoInteractions(gateway);
        }
    }
}
