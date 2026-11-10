package com.markethub.orders.application;

import com.markethub.orders.domain.model.ProductSearchResult;

public record SearchView(ProductSearchResult result, int page, int size, int totalPages) {}

