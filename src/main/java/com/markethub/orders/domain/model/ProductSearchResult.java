package com.markethub.orders.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchResult(List<Hit> hits, long total, List<Facet> brandFacets, List<Facet> categoryFacets) {
    public record Hit(String sku, String name, String brand, BigDecimal price) {}
    public record Facet(String key, long count) {}
}
