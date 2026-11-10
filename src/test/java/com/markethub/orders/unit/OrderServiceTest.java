package com.markethub.orders.unit;

import com.markethub.orders.application.*;
import com.markethub.orders.domain.exception.ConflictException;
import com.markethub.orders.domain.exception.IllegalTransitionException;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.exception.PaymentDeclinedException;
import com.markethub.orders.domain.model.*;
import com.markethub.orders.domain.payment.CardPayment;
import com.markethub.orders.domain.payment.CodPayment;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.payment.PaymentStatus;
import com.markethub.orders.domain.pricing.AppliedPromotion;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.infrastructure.outbox.OutboxWriter;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static com.markethub.orders.OrderFixtures.NOW;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.markethub.orders.OrderFixtures.anOrder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    PricingService pricingService;
    @Mock
    PaymentStrategyFactory strategies;
    @Mock
    PaymentStrategy strategy;
    @Mock
    OrderRepository orders;
    @Mock
    CustomerRepository customers;
    @Mock
    OutboxWriter outbox;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FakeTransactionManager transactionManager = new FakeTransactionManager();
    private final Customer customer = new Customer("buyer@example.com", CustomerTier.SILVER, "KW");
    private OrderService service;


    @BeforeEach
    void setUp() {
        // same order as the fields declared in your OrderService
        service = new OrderService(pricingService, strategies, orders, customers,
            new ViewMapperImpl(), new TransactionTemplate(transactionManager), clock, outbox);
    }

    // placing orders

    @Test
    @DisplayName("an order is numbered from its database id and the clock's year")
    void placeOrder_shouldConfirmAndNumberOrder_whenPaymentSucceeds() {
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.CARD), anyList())).thenReturn(pricedBasket(false));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        savingAssignsId(7L);
        when(strategies.forMethod(PaymentMethod.CARD)).thenReturn(strategy);
        when(strategy.authorise(any(Order.class), any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return new CardPayment(order, order.getTotal(), "1111", "AUTH-1");
        });

        OrderView view = service.placeOrder(command(PaymentMethod.CARD, new CardDetails("4111111111111111", "12/28"), "key-1"));

        assertAll(
            () -> assertThat(view.orderNumber()).isEqualTo("MH-2026-000007"),
            () -> assertThat(view.status()).isEqualTo(OrderStatus.CONFIRMED),
            () -> assertThat(view.total()).isEqualByComparingTo("29.700"),
            () -> assertThat(view.payment().cardLast4()).isEqualTo("1111"),
            () -> assertThat(transactionManager.commits).isEqualTo(1));
        verify(outbox).orderPlaced(any(Order.class));
        verify(outbox).orderStatusChanged(any(Order.class), isNull());
    }

    @Test
    void placeOrder_shouldPutOrderOnHold_whenPricingFlagsFraud() {
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.COD), anyList())).thenReturn(pricedBasket(true));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        savingAssignsId(8L);
        when(strategies.forMethod(PaymentMethod.COD)).thenReturn(strategy);
        when(strategy.authorise(any(Order.class), any()))
            .thenAnswer(invocation -> new CodPayment(invocation.getArgument(0), new BigDecimal("29.700")));

        OrderView view = service.placeOrder(command(PaymentMethod.COD, null, "key-2"));

        assertAll(
            () -> assertThat(view.status()).isEqualTo(OrderStatus.ON_HOLD),
            () -> assertThat(view.payment().status()).isEqualTo(PaymentStatus.PENDING));
        verify(outbox).orderPlaced(any(Order.class));
    }

    @Test
    void placeOrder_shouldPropagateDeclineAndRollBack_whenPaymentIsDeclined() {
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.CARD), anyList())).thenReturn(pricedBasket(false));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        savingAssignsId(9L);
        when(strategies.forMethod(PaymentMethod.CARD)).thenReturn(strategy);
        when(strategy.authorise(any(Order.class), any())).thenThrow(new PaymentDeclinedException("declined"));

        assertThatThrownBy(() -> service.placeOrder(
            command(PaymentMethod.CARD, new CardDetails("4111111111110002", "12/28"), "key-3")))
            .isInstanceOf(PaymentDeclinedException.class);

        assertAll(
            () -> assertThat(transactionManager.rollbacks).isEqualTo(1),
            () -> assertThat(transactionManager.commits).isZero());
        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("the same key and body replay the first order without touching payment")
    void placeOrder_shouldReplayOriginalOrder_whenSameKeyAndBodyArriveAgain() {
        PlaceOrderCommand command = command(PaymentMethod.CARD, new CardDetails("4111111111111111", "12/28"), "key-4");
        AtomicReference<Order> sent = duplicateKeyOnSave();
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.CARD), anyList())).thenReturn(pricedBasket(false));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        when(orders.findWithDetailsByIdempotencyKey("key-4")).thenAnswer(invocation -> Optional.of(
            anOrder().withOrderNumber("MH-2026-000001").withRequestHash(sent.get().getRequestHash()).build()));

        OrderView view = service.placeOrder(command);

        assertThat(view.orderNumber()).isEqualTo("MH-2026-000001");
        verifyNoInteractions(strategies);
        verifyNoInteractions(outbox);
    }

    @Test
    void placeOrder_shouldRejectReuse_whenSameKeyComesWithDifferentBody() {
        PlaceOrderCommand command = command(PaymentMethod.CARD, new CardDetails("4111111111111111", "12/28"), "key-5");
        duplicateKeyOnSave();
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.CARD), anyList())).thenReturn(pricedBasket(false));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        when(orders.findWithDetailsByIdempotencyKey("key-5"))
            .thenReturn(Optional.of(anOrder().withRequestHash("b".repeat(64)).build()));

        assertThatThrownBy(() -> service.placeOrder(command))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
        verifyNoInteractions(strategies);
        verifyNoInteractions(outbox);
    }

    @Test
    void placeOrder_shouldRethrow_whenAnotherConstraintFailsInsteadOfTheIdempotencyKey() {
        PlaceOrderCommand command = command(PaymentMethod.CARD, new CardDetails("4111111111111111", "12/28"), "key-6");
        duplicateKeyOnSave();
        when(pricingService.priceBasket(eq(1L), any(), eq(PaymentMethod.CARD), anyList())).thenReturn(pricedBasket(false));
        when(customers.getReferenceById(1L)).thenReturn(customer);
        when(orders.findWithDetailsByIdempotencyKey("key-6")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder(command)).isInstanceOf(DataIntegrityViolationException.class);
        verifyNoInteractions(outbox);
    }

    // ---------- reading, cancelling, moving orders ----------

    @Test
    void getOrder_shouldThrowNotFound_whenOrderDoesNotExist() {
        when(orders.findWithDetailsByOrderNumber("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder("NOPE")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancel_shouldCancelOrderAndRefundPayment_whenOrderIsConfirmed() {
        Order order = anOrder().withStatus(OrderStatus.CONFIRMED).build();
        when(orders.findWithDetailsByOrderNumber("MH-2026-000001")).thenReturn(Optional.of(order));
        when(strategies.forMethod(PaymentMethod.CARD)).thenReturn(strategy);

        OrderView view = service.cancel("MH-2026-000001", "changed my mind");

        verify(strategy).refund(order.getPayment());
        assertAll(
            () -> assertThat(view.status()).isEqualTo(OrderStatus.CANCELLED),
            () -> assertThat(view.payment().status()).isEqualTo(PaymentStatus.REFUNDED));
        verify(outbox).orderStatusChanged(order, "changed my mind");
    }

    @Test
    @DisplayName("no money moves when the cancel itself is illegal")
    void cancel_shouldRejectAndRefundNothing_whenOrderIsShipped() {
        Order order = anOrder().withStatus(OrderStatus.SHIPPED).build();
        when(orders.findWithDetailsByOrderNumber("MH-2026-000001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel("MH-2026-000001", "too late"))
            .isInstanceOf(IllegalTransitionException.class);
        verifyNoInteractions(strategies);
        verifyNoInteractions(outbox);
    }

    @Test
    void transition_shouldShipOrder_whenOrderIsConfirmed() {
        Order order = anOrder().withStatus(OrderStatus.CONFIRMED).build();
        when(orders.findWithDetailsByOrderNumber("MH-2026-000001")).thenReturn(Optional.of(order));

        OrderView view = service.transition("MH-2026-000001", OrderAction.SHIP);

        assertThat(view.status()).isEqualTo(OrderStatus.SHIPPED);
        verify(outbox).orderStatusChanged(order, null);
    }

    @Test
    void transition_shouldReject_whenShippingAnOrderOnHold() {
        Order order = anOrder().withStatus(OrderStatus.ON_HOLD).build();
        when(orders.findWithDetailsByOrderNumber("MH-2026-000001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.transition("MH-2026-000001", OrderAction.SHIP))
            .isInstanceOf(IllegalTransitionException.class);
    }

    // ---------- history ----------

    @Test
    void history_shouldThrowNotFound_whenCustomerDoesNotExist() {
        when(customers.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.history(99L, null, 0, 20)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void history_shouldCapPageSizeAndFixNegativePage_whenValuesAreOutOfRange() {
        when(customers.existsById(1L)).thenReturn(true);
        when(orders.findSummaries(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        service.history(1L, null, -3, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orders).findSummaries(eq(1L), pageable.capture());
        assertAll(
            () -> assertThat(pageable.getValue().getPageSize()).isEqualTo(100),
            () -> assertThat(pageable.getValue().getPageNumber()).isZero());
    }

    @Test
    void history_shouldFilterByStatus_whenStatusIsGiven() {
        when(customers.existsById(1L)).thenReturn(true);
        when(orders.findSummariesByStatus(eq(1L), eq(OrderStatus.SHIPPED), any(Pageable.class))).thenReturn(Page.empty());

        service.history(1L, OrderStatus.SHIPPED, 0, 20);

        verify(orders, never()).findSummaries(any(), any());
    }

    // ---------- helpers ----------

    private PlaceOrderCommand command(PaymentMethod method, CardDetails card, String key) {
        return new PlaceOrderCommand(1L, "WELCOME10", method, card,
            new Address("KW", "Kuwait City", "Block 3, Street 12"),
            List.of(new RequestedLine("MH-OUD-50", 2), new RequestedLine("MH-ROSE-30", 1)), key);
    }

    private static PricedBasket pricedBasket(boolean onHold) {
        PricingResult pricing = new PricingResult(new BigDecimal("33.000"), new BigDecimal("3.300"),
            new BigDecimal("0.000"), new BigDecimal("29.700"),
            List.of(new AppliedPromotion("R2", "WELCOME10 coupon", new BigDecimal("3.300"))), onHold);
        return new PricedBasket(pricing, List.of(
            new PricedBasket.PricedLine("MH-OUD-50", "Royal Oud 50ml", 2, new BigDecimal("12.500")),
            new PricedBasket.PricedLine("MH-ROSE-30", "Damask Rose 30ml", 1, new BigDecimal("8.000"))));
    }

    /** Saving gives the order the id the database would have given it. */
    private void savingAssignsId(long id) {
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });
    }

    /** Saving fails like a unique-constraint violation, and remembers what was sent. */
    private AtomicReference<Order> duplicateKeyOnSave() {
        AtomicReference<Order> sent = new AtomicReference<>();
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            sent.set(invocation.getArgument(0));
            throw new DataIntegrityViolationException("duplicate key");
        });
        return sent;
    }

    private static final class FakeTransactionManager implements PlatformTransactionManager {
        int commits;
        int rollbacks;

        @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return new SimpleTransactionStatus(); }
        @Override public void commit(TransactionStatus status) { commits++; }
        @Override public void rollback(TransactionStatus status) { rollbacks++; }
    }
}
