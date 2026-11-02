package com.markethub.orders.domain.payment;

import com.markethub.orders.domain.model.Money;
import com.markethub.orders.domain.model.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "method")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(precision = 12, scale = 3)
    private BigDecimal amount;
    private String currency = Money.CURRENCY;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected Payment(Order order, BigDecimal amount, PaymentStatus status) {
        this.order = order;
        this.amount = new Money(amount).amount();
        this.status = status;
    }
}

