package com.markethub.orders.domain.payment;

import com.markethub.orders.domain.model.Order;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity @DiscriminatorValue("COD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodPayment extends Payment {
    public CodPayment(Order order, BigDecimal amount) { super(order, amount, PaymentStatus.PENDING); }

    @Override public PaymentMethod method() { return PaymentMethod.COD; }
}
