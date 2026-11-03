package com.markethub.orders.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)     // many lines belong to one order
    @JoinColumn(name = "order_id")         // the foreign key column
    private Order order;

    private String sku;
    private String name;
    private int quantity;
    @Column(precision = 12, scale = 3)
    private BigDecimal unitPrice;

    public OrderLine(String sku, String name, int quantity, BigDecimal unitPrice) {
        this.sku = sku; this.name = name; this.quantity = quantity;
        this.unitPrice = new Money(unitPrice).amount();
    }

    void setOrder(Order order) { this.order = order; }   // package-private: only Order may call it
}
