package com.markethub.orders.web.dto;

public record ProductResponse(String sku, String nameEn, String nameAr, String brand, String category,
                              String price, String currency, String status, Long version) {}
