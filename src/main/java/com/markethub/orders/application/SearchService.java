package com.markethub.orders.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.markethub.orders.domain.model.ProductSearchQuery;
import com.markethub.orders.domain.model.ProductSearchResult;
import com.markethub.orders.domain.model.SearchSort;
import com.markethub.orders.infrastructure.search.ProductSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductSearchRepository repository;

    public SearchView search(String text, List<String> brands, List<String> categories,
                             BigDecimal minPrice, BigDecimal maxPrice, String sort, Integer page, Integer size) {
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);   // capped at 100 [Doc]
        ProductSearchQuery query = new ProductSearchQuery(
            text == null || text.isBlank() ? null : text.trim(),
            brands == null ? List.of() : brands,
            categories == null ? List.of() : categories,
            minPrice, maxPrice, SearchSort.fromParam(sort), pageNumber, pageSize);

        ProductSearchResult result = repository.search(query);
        int totalPages = (int) ((result.total() + pageSize - 1) / pageSize);
        return new SearchView(result, pageNumber, pageSize, totalPages);
    }
}
