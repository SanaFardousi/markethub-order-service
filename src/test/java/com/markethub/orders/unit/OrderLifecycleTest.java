package com.markethub.orders.unit;

import com.markethub.orders.domain.exception.IllegalTransitionException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.markethub.orders.OrderFixtures.NOW;
import static com.markethub.orders.OrderFixtures.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderLifecycleTest {

    private static final Instant LATER = NOW.plusSeconds(60);

    @Nested
    @DisplayName("an order on hold")
    class WhenOnHold {
        private final Order order = anOrder().withStatus(OrderStatus.ON_HOLD).withCashOnDelivery().build();

        @Test
        void approve_shouldConfirmAndStampTheTime_whenOrderIsOnHold() {
            order.approve(LATER);

            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED),
                () -> assertThat(order.getUpdatedAt()).isEqualTo(LATER));
        }

        @Test
        void cancel_shouldCancelAndLeavePaymentPending_whenOrderIsOnHold() {
            order.cancel(LATER);

            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING));
        }

        @Test
        void ship_shouldReject_whenOrderIsOnHold() {
            assertThatThrownBy(() -> order.ship(LATER)).isInstanceOf(IllegalTransitionException.class);

            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ON_HOLD),
                () -> assertThat(order.getUpdatedAt()).isEqualTo(NOW));
        }
    }

    @Nested
    @DisplayName("a confirmed order")
    class WhenConfirmed {
        private final Order paidByCard = anOrder().withStatus(OrderStatus.CONFIRMED).build();
        private final Order cashOnDelivery = anOrder().withStatus(OrderStatus.CONFIRMED).withCashOnDelivery().build();

        @Test
        void ship_shouldMoveToShipped_whenOrderIsConfirmed() {
            paidByCard.ship(LATER);

            assertThat(paidByCard.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        void cancel_shouldRefundPayment_whenOrderWasPaidByCard() {
            paidByCard.cancel(LATER);

            assertAll(
                () -> assertThat(paidByCard.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(paidByCard.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED));
        }

        @Test
        void cancel_shouldLeavePaymentPending_whenOrderIsCashOnDelivery() {
            cashOnDelivery.cancel(LATER);

            assertThat(cashOnDelivery.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        void approve_shouldReject_whenOrderIsAlreadyConfirmed() {
            assertThatThrownBy(() -> paidByCard.approve(LATER)).isInstanceOf(IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("a shipped order")
    class WhenShipped {
        private final Order order = anOrder().withStatus(OrderStatus.SHIPPED).build();

        @Test
        void deliver_shouldMoveToDelivered_whenOrderIsShipped() {
            order.deliver(LATER);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        void cancel_shouldReject_whenOrderIsShipped() {
            assertThatThrownBy(() -> order.cancel(LATER)).isInstanceOf(IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("a delivered order")
    class WhenDelivered {
        private final Order order = anOrder().withStatus(OrderStatus.DELIVERED).build();

        @Test
        void ship_shouldReject_whenOrderIsDelivered() {
            assertThatThrownBy(() -> order.ship(LATER)).isInstanceOf(IllegalTransitionException.class);
        }

        @Test
        void cancel_shouldReject_whenOrderIsDelivered() {
            assertThatThrownBy(() -> order.cancel(LATER)).isInstanceOf(IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("a cancelled order")
    class WhenCancelled {
        private final Order order = anOrder().withStatus(OrderStatus.CANCELLED).build();

        @Test
        void approve_shouldReject_whenOrderIsCancelled() {
            assertThatThrownBy(() -> order.approve(LATER)).isInstanceOf(IllegalTransitionException.class);
        }

        @Test
        void cancel_shouldReject_whenOrderIsAlreadyCancelled() {
            assertThatThrownBy(() -> order.cancel(LATER)).isInstanceOf(IllegalTransitionException.class);
        }
    }
}
