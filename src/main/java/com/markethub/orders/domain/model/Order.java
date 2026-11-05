package com.markethub.orders.domain.model;

import com.markethub.orders.domain.payment.Payment;
import com.markethub.orders.domain.pricing.AppliedPromotion;
import com.markethub.orders.domain.state.OrderState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")                    // the table is "orders", not "order"
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String idempotencyKey;
    private String requestHash;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String couponCode;

    @Column(precision = 12, scale = 3) private BigDecimal subtotal = Money.ZERO.amount();
    @Column(precision = 12, scale = 3) private BigDecimal discount = Money.ZERO.amount();
    @Column(precision = 12, scale = 3) private BigDecimal shipping = Money.ZERO.amount();
    @Column(precision = 12, scale = 3) private BigDecimal total    = Money.ZERO.amount();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "country", column = @Column(name = "ship_country")),
        @AttributeOverride(name = "city",    column = @Column(name = "ship_city")),
        @AttributeOverride(name = "line1",   column = @Column(name = "ship_line1"))
    })
    private Address shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;

    public Order(String orderNumber, Customer customer, String idempotencyKey, String requestHash,
                 String couponCode, Address shippingAddress, OrderStatus status, Instant now) {
        this.orderNumber = orderNumber; this.customer = customer;
        this.idempotencyKey = idempotencyKey; this.requestHash = requestHash;
        this.couponCode = couponCode; this.shippingAddress = shippingAddress;
        this.status = status; this.createdAt = now; this.updatedAt = now;
    }

    /** The only way to add a line: it sets both sides of the relationship. */
    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    public void attachPayment(Payment payment) { this.payment = payment; }

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AppliedPromotion> promotions = new ArrayList<>();

    public void approve(Instant now) { moveTo(OrderState.of(status).approve(this), now); }
    public void ship(Instant now)    { moveTo(OrderState.of(status).ship(this), now); }
    public void deliver(Instant now) { moveTo(OrderState.of(status).deliver(this), now); }
    public void cancel(Instant now)  { moveTo(OrderState.of(status).cancel(this), now); }

    public void requestRefund() {
        if (payment != null) {
            payment.markRefundedIfPaid();
        }
    }

    private void moveTo(OrderState next, Instant now) {
        this.status = next.status();
        this.updatedAt = now;
    }

}
