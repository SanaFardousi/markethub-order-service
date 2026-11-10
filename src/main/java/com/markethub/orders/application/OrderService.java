package com.markethub.orders.application;

import com.markethub.orders.domain.exception.ConflictException;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderAction;
import com.markethub.orders.domain.model.OrderLine;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.payment.Payment;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.infrastructure.outbox.OutboxWriter;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final PricingService pricingService;
    private final PaymentStrategyFactory strategies;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ViewMapper viewMapper;
    private final TransactionTemplate transactions;
    private final Clock clock;

    private final OutboxWriter outbox;

    public OrderView placeOrder(PlaceOrderCommand command) {
        try {
            return transactions.execute(status -> create(command));
        } catch (DataIntegrityViolationException duplicate) {
            return replayOrReject(command, duplicate);
        }
    }

    private OrderView create(PlaceOrderCommand command) {
        PricedBasket priced = pricingService.priceBasket(command.customerId(), command.couponCode(),
            command.paymentMethod(), command.lines());
        PricingResult pricing = priced.pricing();
        Instant now = clock.instant();

        Order order = new Order(placeholderNumber(), customers.getReferenceById(command.customerId()),
            command.idempotencyKey(), sha256Hex(command.fingerprint()), command.couponCode(),
            command.shippingAddress(), pricing.onHold() ? OrderStatus.ON_HOLD : OrderStatus.CONFIRMED, now);
        priced.lines().forEach(l -> order.addLine(new OrderLine(l.sku(), l.name(), l.quantity(), l.unitPrice())));
        order.applyPricing(pricing);

        orders.saveAndFlush(order);                    // claims the idempotency key; a duplicate fails here
        order.assignOrderNumber(finalNumber(order.getId()));

        Payment payment = strategies.forMethod(command.paymentMethod()).authorise(order, command.card());
        order.attachPayment(payment);
        outbox.orderPlaced(order);
        outbox.orderStatusChanged(order, null);
        return viewMapper.toView(order);
    }

    private OrderView replayOrReject(PlaceOrderCommand command, DataIntegrityViolationException duplicate) {
        return transactions.execute(status -> {
            Order existing = orders.findWithDetailsByIdempotencyKey(command.idempotencyKey())
                .orElseThrow(() -> duplicate);
            if (!existing.getRequestHash().equals(sha256Hex(command.fingerprint()))) {
                throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was already used with a different request body");
            }
            return viewMapper.toView(existing);
        });
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(String orderNumber) { return viewMapper.toView(load(orderNumber)); }

    @Transactional(readOnly = true)
    public Page<OrderSummary> history(Long customerId, OrderStatus status, int page, int size) {
        if (!customers.existsById(customerId)) {
            throw new NotFoundException("Customer " + customerId + " not found");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return status == null ? orders.findSummaries(customerId, pageable)
            : orders.findSummariesByStatus(customerId, status, pageable);
    }

    @Transactional
    public OrderView cancel(String orderNumber, String reason) {
        Order order = load(orderNumber);
        order.cancel(clock.instant());
        Payment payment = order.getPayment();
        strategies.forMethod(payment.method()).refund(payment);
        outbox.orderStatusChanged(order, reason);
        return viewMapper.toView(order);
    }

    @Transactional
    public OrderView transition(String orderNumber, OrderAction action) {
        Order order = load(orderNumber);
        action.applyTo(order, clock.instant());
        outbox.orderStatusChanged(order, null);
        return viewMapper.toView(order);
    }

    private Order load(String orderNumber) {
        return orders.findWithDetailsByOrderNumber(orderNumber)
            .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " not found"));
    }

    private static String placeholderNumber() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private String finalNumber(Long id) {
        return String.format("MH-%d-%06d", Year.now(clock).getValue(), id);
    }

    private static String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
