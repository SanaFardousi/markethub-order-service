package com.markethub.orders.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchQuery(String text, List<String> brands, List<String> categories,
                                 BigDecimal minPrice, BigDecimal maxPrice, SearchSort sort, int page, int size) {}
