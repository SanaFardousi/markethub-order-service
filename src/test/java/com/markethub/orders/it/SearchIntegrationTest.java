package com.markethub.orders.it;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.jayway.jsonpath.JsonPath;
import com.markethub.orders.infrastructure.search.ProductDocument;
import com.markethub.orders.infrastructure.search.ProductIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired ElasticsearchClient es;
    @Autowired ProductIndexer indexer;

    private String run;
    private String category;

    private String sku(int n) { return "TEST-S" + n + "-" + run; }

    private ProductDocument doc(String sku, String nameEn, String nameAr, String brand, String category,
                                String price, String status) {
        return new ProductDocument(sku, nameEn, nameAr, brand, category, status, new BigDecimal(price), 1);
    }

    @BeforeEach
    void indexFixtures() throws Exception {
        run = UUID.randomUUID().toString().substring(0, 8);
        category = "tc-" + run;
        indexer.index(doc(sku(1), "Royal Oud perfume 50ml", "عود ملكي 50 مل", "TESTBRAND-A", category, "12.500", "ACTIVE"));
        indexer.index(doc(sku(2), "Midnight Rose 30ml", "ورد منتصف الليل 30 مل", "TESTBRAND-A", category, "8.000", "ACTIVE"));
        indexer.index(doc(sku(3), "Oud Intense 100ml", "عود مكثف 100 مل", "TESTBRAND-B", category, "30.000", "ACTIVE"));
        indexer.index(doc(sku(4), "Amber Musk 50ml", "مسك عنبر 50 مل", "TESTBRAND-B", category, "20.000", "ACTIVE"));
        indexer.index(doc(sku(5), "Hidden Oud 50ml", "عود مخفي 50 مل", "TESTBRAND-A", category, "5.000", "INACTIVE"));
        es.indices().refresh(r -> r.index(ProductIndexer.ALIAS));       // make the new documents searchable now
    }

    /** Pairs of name, value. The category filter is always added. */
    private String search(String... params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/search/products").param("category", category);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<String> skus(String json) { return JsonPath.read(json, "$.content[*].sku"); }

    @Test
    void search_shouldFindEnglishMatches_andHideInactiveProducts() throws Exception {
        assertThat(skus(search("q", "oud"))).containsExactlyInAnyOrder(sku(1), sku(3));
        assertThat(skus(search("q", "hidden"))).isEmpty();
    }

    @Test
    @DisplayName("an Arabic query matches the Arabic names")
    void search_shouldFindArabicMatches() throws Exception {
        assertThat(skus(search("q", "عود"))).containsExactlyInAnyOrder(sku(1), sku(3));
    }

    @Test
    void search_shouldFilterByBrand_alone_andByAnyOfSeveralBrands() throws Exception {
        assertThat(skus(search("brand", "TESTBRAND-A"))).containsExactlyInAnyOrder(sku(1), sku(2));
        assertThat(skus(search("brand", "TESTBRAND-A", "brand", "TESTBRAND-B")))
            .containsExactlyInAnyOrder(sku(1), sku(2), sku(3), sku(4));
    }

    @Test
    void search_shouldFilterByPriceRange_withInclusiveBounds() throws Exception {
        assertThat(skus(search("minPrice", "12.500", "maxPrice", "20.000"))).containsExactlyInAnyOrder(sku(1), sku(4));
        assertThat(skus(search("minPrice", "25.000"))).containsExactly(sku(3));
    }

    @Test
    void search_shouldCombineTextAndFilters_withAnd() throws Exception {
        assertThat(skus(search("q", "oud", "brand", "TESTBRAND-B", "maxPrice", "40.000"))).containsExactly(sku(3));
    }

    @Test
    void search_shouldReturnEverythingActiveWithFacets_whenOnlyTheCategoryFilterIsGiven() throws Exception {
        String json = search();

        assertAll(
            () -> assertThat(skus(json)).hasSize(4),
            () -> assertThat(JsonPath.<List<Integer>>read(json, "$.facets.brand[?(@.key=='TESTBRAND-A')].count")).containsExactly(2),
            () -> assertThat(JsonPath.<List<Integer>>read(json, "$.facets.brand[?(@.key=='TESTBRAND-B')].count")).containsExactly(2),
            () -> assertThat(JsonPath.<List<Integer>>read(json, "$.facets.category[?(@.key=='" + category + "')].count")).containsExactly(4));
    }

    @Test
    void search_shouldSortByPrice_ascendingAndDescending() throws Exception {
        assertThat(skus(search("sort", "price_asc"))).containsExactly(sku(2), sku(1), sku(4), sku(3));
        assertThat(skus(search("sort", "price_desc"))).containsExactly(sku(3), sku(4), sku(1), sku(2));
    }

    @Test
    void search_shouldPage_andReportTotals() throws Exception {
        String second = search("sort", "price_asc", "size", "2", "page", "1");

        assertAll(
            () -> assertThat(skus(second)).containsExactly(sku(4), sku(3)),
            () -> assertThat(JsonPath.<Integer>read(second, "$.totalElements")).isEqualTo(4),
            () -> assertThat(JsonPath.<Integer>read(second, "$.totalPages")).isEqualTo(2),
            () -> assertThat(JsonPath.<Integer>read(second, "$.page")).isEqualTo(1),
            () -> assertThat(JsonPath.<Integer>read(second, "$.size")).isEqualTo(2));
    }

    @Test
    void search_shouldCapPageSizeAt100() throws Exception {
        assertThat(JsonPath.<Integer>read(search("size", "500"), "$.size")).isEqualTo(100);
    }

    @Test
    void search_shouldReturn400_whenSortIsUnknown() throws Exception {
        mvc.perform(get("/api/v1/search/products").param("sort", "popularity"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void search_shouldReturnEveryActiveProduct_whenNoFilterAtAll() throws Exception {
        String json = mvc.perform(get("/api/v1/search/products?size=5")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertAll(
            () -> assertThat(JsonPath.<Integer>read(json, "$.totalElements")).isGreaterThanOrEqualTo(100),
            () -> assertThat(skus(json)).hasSize(5));
    }

    @Test
    @DisplayName("the startup bulk load indexed the seeded products")
    void startup_shouldHaveBulkIndexedTheSeedProducts() throws Exception {
        ProductDocument oud = es.get(g -> g.index(ProductIndexer.ALIAS).id("MH-OUD-50"), ProductDocument.class).source();

        assertAll(
            () -> assertThat(oud).isNotNull(),
            () -> assertThat(oud.nameEn()).isEqualTo("Royal Oud 50ml"),
            () -> assertThat(oud.price()).isEqualByComparingTo("12.500"));
    }

    @Test
    @DisplayName("a repeated or older event never overwrites a newer document")
    void index_shouldKeepTheNewestVersion_whenEventsAreRepeatedOrArriveOutOfOrder() throws Exception {
        String sku = "TEST-VER-" + run;
        indexer.index(new ProductDocument(sku, "V", "V", "X", category, "ACTIVE", new BigDecimal("5.000"), 5));
        indexer.index(new ProductDocument(sku, "V", "V", "X", category, "ACTIVE", new BigDecimal("6.000"), 5));   // same version again
        indexer.index(new ProductDocument(sku, "V", "V", "X", category, "ACTIVE", new BigDecimal("7.000"), 3));   // older version

        ProductDocument kept = es.get(g -> g.index(ProductIndexer.ALIAS).id(sku), ProductDocument.class).source();
        assertAll(
            () -> assertThat(kept.price()).isEqualByComparingTo("5.000"),
            () -> assertThat(kept.version()).isEqualTo(5));

        indexer.index(new ProductDocument(sku, "V", "V", "X", category, "ACTIVE", new BigDecimal("8.000"), 6)); // newer
        ProductDocument updated = es.get(g -> g.index(ProductIndexer.ALIAS).id(sku), ProductDocument.class).source();
        assertThat(updated.price()).isEqualByComparingTo("8.000");
    }

    @Test
    @DisplayName("a price changed through the admin endpoint shows up in search within seconds")
    void upsertProduct_shouldShowTheNewPriceInSearch_whenPriceChanges() throws Exception {
        String sku = "TEST-UP-" + run;
        String cat = "tc-up-" + run;

        putProduct(sku, cat, "12.500");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(priceOf(cat)).isEqualTo("12.500"));

        putProduct(sku, cat, "9.900");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(priceOf(cat)).isEqualTo("9.900"));
    }

    private void putProduct(String sku, String cat, String price) throws Exception {
        mvc.perform(put("/api/v1/admin/products/" + sku).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nameEn": "Test Up", "nameAr": "test", "brand": "MAISON", "category": "%s",
                         "price": %s, "status": "ACTIVE"}""".formatted(cat, price)))
            .andExpect(status().isOk());
    }

    private String priceOf(String cat) throws Exception {
        String json = mvc.perform(get("/api/v1/search/products").param("category", cat))
            .andReturn().getResponse().getContentAsString();
        List<String> prices = JsonPath.read(json, "$.content[*].price");
        return prices.isEmpty() ? null : prices.get(0);
    }
}
