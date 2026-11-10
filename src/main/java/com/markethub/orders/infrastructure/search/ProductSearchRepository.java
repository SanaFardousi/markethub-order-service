package com.markethub.orders.infrastructure.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.markethub.orders.domain.model.ProductSearchQuery;
import com.markethub.orders.domain.model.ProductSearchResult;
import com.markethub.orders.domain.model.SearchSort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductSearchRepository {

    private static final int FACET_LIMIT = 50;
    private static final SortOptions BY_SKU = SortOptions.of(s -> s.field(f -> f.field("sku").order(SortOrder.Asc)));
    private static final Map<SearchSort, List<SortOptions>> SORTS = Map.of(
        SearchSort.RELEVANCE, List.of(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))), BY_SKU),
        SearchSort.PRICE_ASC, List.of(SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Asc))), BY_SKU),
        SearchSort.PRICE_DESC, List.of(SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Desc))), BY_SKU));

    private final ElasticsearchClient client;

    public ProductSearchResult search(ProductSearchQuery q) {
        SearchRequest request = new SearchRequest.Builder()
            .index(ProductIndexer.ALIAS)
            .from(q.page() * q.size())
            .size(q.size())
            .trackTotalHits(t -> t.enabled(true))
            .query(buildQuery(q))
            .sort(SORTS.get(q.sort()))
            .aggregations("brand", a -> a.terms(t -> t.field("brand").size(FACET_LIMIT)))
            .aggregations("category", a -> a.terms(t -> t.field("category").size(FACET_LIMIT)))
            .build();
        try {
            return toResult(client.search(request, ProductDocument.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Query buildQuery(ProductSearchQuery q) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (q.text() != null) {
            bool.must(m -> m.multiMatch(mm -> mm.query(q.text()).fields("name_en", "name_ar")));   // scored
        }
        bool.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));                          // filters: no score
        if (!q.brands().isEmpty()) {
            bool.filter(f -> f.terms(t -> t.field("brand").terms(v -> v.value(values(q.brands())))));
        }
        if (!q.categories().isEmpty()) {
            bool.filter(f -> f.terms(t -> t.field("category").terms(v -> v.value(values(q.categories())))));
        }
        if (q.minPrice() != null || q.maxPrice() != null) {
            bool.filter(f -> f.range(r -> r.number(n -> {
                n.field("price");
                if (q.minPrice() != null) n.gte(q.minPrice().doubleValue());
                if (q.maxPrice() != null) n.lte(q.maxPrice().doubleValue());
                return n;
            })));
        }
        return bool.build()._toQuery();
    }

    private static List<FieldValue> values(List<String> texts) {
        return texts.stream().map(FieldValue::of).toList();
    }

    private static ProductSearchResult toResult(SearchResponse<ProductDocument> response) {
        List<ProductSearchResult.Hit> hits = response.hits().hits().stream()
            .map(h -> h.source())
            .map(d -> new ProductSearchResult.Hit(d.sku(), d.nameEn(), d.brand(), d.price()))
            .toList();
        long total = response.hits().total() == null ? hits.size() : response.hits().total().value();
        return new ProductSearchResult(hits, total, facets(response, "brand"), facets(response, "category"));
    }

    private static List<ProductSearchResult.Facet> facets(SearchResponse<?> response, String name) {
        return response.aggregations().get(name).sterms().buckets().array().stream()
            .map(b -> new ProductSearchResult.Facet(b.key().stringValue(), b.docCount()))
            .toList();
    }
}
