package com.markethub.orders.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String nameEn;
    private String nameAr;
    private String brand;
    private String category;
    @Column(precision = 12, scale = 3)
    private BigDecimal priceAmount;
    private String currency = Money.CURRENCY;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    @Version
    private Long version;
    private Instant updatedAt;

    public Product(String sku, String nameEn, String nameAr, String brand, String category,
                   BigDecimal price, ProductStatus status, Instant now) {
        this.sku = sku; this.nameEn = nameEn; this.nameAr = nameAr;
        this.brand = brand; this.category = category;
        this.priceAmount = new Money(price).amount();
        this.status = status; this.updatedAt = now;
    }
}
