package com.markethub.orders.infrastructure.search;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.markethub.orders.domain.model.Product;

public record ProductDocument(
    String sku,
    @JsonProperty("name_en") String nameEn,
    @JsonProperty("name_ar") String nameAr,
    String brand, String category, String status,
    BigDecimal price, long version) {

    public static ProductDocument from(Product p) {
        return new ProductDocument(p.getSku(), p.getNameEn(), p.getNameAr(), p.getBrand(), p.getCategory(),
            p.getStatus().name(), p.getPriceAmount(), p.getVersion());
    }
}
