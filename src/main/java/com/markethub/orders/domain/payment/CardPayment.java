package com.markethub.orders.domain.payment;

import com.markethub.orders.domain.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity @DiscriminatorValue("CARD")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardPayment extends Payment {
    @Column(name = "card_last4") private String cardLast4;
    @Column(name = "auth_code")  private String authCode;

    public CardPayment(Order order, BigDecimal amount, String cardLast4, String authCode) {
        super(order, amount, PaymentStatus.AUTHORISED);
        this.cardLast4 = cardLast4; this.authCode = authCode;
    }
}
