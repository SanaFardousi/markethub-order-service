package com.markethub.orders.domain.payment;

import com.markethub.orders.domain.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity @DiscriminatorValue("WALLET")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletPayment extends Payment {
    @Column(name = "wallet_txn_id") private String walletTxnId;

    public WalletPayment(Order order, BigDecimal amount, String walletTxnId) {
        super(order, amount, PaymentStatus.AUTHORISED);
        this.walletTxnId = walletTxnId;
    }
}
