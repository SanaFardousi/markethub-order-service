package com.markethub.orders.web;
import java.util.List;

import com.markethub.orders.application.SearchView;
import com.markethub.orders.domain.model.Money;
import com.markethub.orders.domain.model.ProductSearchResult;

public record SearchResponse(List<Hit> content, int page, int size, long totalElements, int totalPages, Facets facets) {
    public record Hit(String sku, String name, String brand, String price) {}
    public record Facet(String key, long count) {}
    public record Facets(List<Facet> brand, List<Facet> category) {}

    public static SearchResponse from(SearchView view) {
        ProductSearchResult r = view.result();
        return new SearchResponse(
            r.hits().stream().map(h -> new Hit(h.sku(), h.name(), h.brand(),
                new Money(h.price()).amount().toPlainString())).toList(),
            view.page(), view.size(), r.total(), view.totalPages(),
            new Facets(r.brandFacets().stream().map(f -> new Facet(f.key(), f.count())).toList(),
                r.categoryFacets().stream().map(f -> new Facet(f.key(), f.count())).toList()));
    }
}




