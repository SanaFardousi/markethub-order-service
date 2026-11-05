package com.markethub.orders.application;

import com.markethub.orders.domain.model.ProductStatus;

import java.math.BigDecimal;

public record ProductView(String sku, String nameEn, String nameAr, String brand, String category,
                          BigDecimal price, String currency, ProductStatus status, Long version) {}
