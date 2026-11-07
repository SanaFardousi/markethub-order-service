package com.markethub.orders.unit;

import com.markethub.orders.domain.exception.IllegalTransitionException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.markethub.orders.OrderFixtures.NOW;
import static com.markethub.orders.OrderFixtures.anOrder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OrderStateTest {

    private static final Map<String, BiConsumer<Order, Instant>> ACTIONS = new LinkedHashMap<>();
    static {
        ACTIONS.put("APPROVE", Order::approve);
        ACTIONS.put("SHIP", Order::ship);
        ACTIONS.put("DELIVER", Order::deliver);
        ACTIONS.put("CANCEL", Order::cancel);
    }

    private static final Set<String> ALLOWED = Set.of(
        "ON_HOLD/APPROVE", "ON_HOLD/CANCEL", "CONFIRMED/SHIP", "CONFIRMED/CANCEL", "SHIPPED/DELIVER");

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @CsvSource({
        "ON_HOLD,   APPROVE, CONFIRMED",
        "ON_HOLD,   CANCEL,  CANCELLED",
        "CONFIRMED, SHIP,    SHIPPED",
        "CONFIRMED, CANCEL,  CANCELLED",
        "SHIPPED,   DELIVER, DELIVERED" })
    void action_shouldMoveOrder_whenTransitionIsAllowed(OrderStatus from, String action, OrderStatus expected) {
        Order order = anOrder().withStatus(from).build();

        ACTIONS.get(action).accept(order, NOW);

        assertThat(order.getStatus()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} is refused")
    @MethodSource("illegalPairs")
    void action_shouldThrowIllegalTransition_whenTransitionIsNotAllowed(OrderStatus from, String action) {
        Order order = anOrder().withStatus(from).build();

        assertThatThrownBy(() -> ACTIONS.get(action).accept(order, NOW))
            .isInstanceOfSatisfying(IllegalTransitionException.class,
                e -> assertThat(e.getCode()).isEqualTo("ILLEGAL_TRANSITION"));
        assertThat(order.getStatus()).isEqualTo(from);   // and nothing changed
    }

    static Stream<Arguments> illegalPairs() {
        return Arrays.stream(OrderStatus.values())
            .flatMap(status -> ACTIONS.keySet().stream()
                .filter(action -> !ALLOWED.contains(status + "/" + action))
                .map(action -> Arguments.of(status, action)));
    }
}
