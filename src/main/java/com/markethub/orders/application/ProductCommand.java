package com.markethub.orders.application;

import com.markethub.orders.domain.model.ProductStatus;

import java.math.BigDecimal;

public record ProductCommand(String sku, String nameEn, String nameAr, String brand, String category,
                             BigDecimal price, ProductStatus status) {}
