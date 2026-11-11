package com.markethub.orders.web;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.markethub.orders.application.SearchService;
import com.markethub.orders.application.SearchView;
import com.markethub.orders.domain.model.ProductSearchResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(FixedClockConfig.class)
class SearchControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean SearchService searchService;

    @Test
    void search_shouldReturnTheDocumentedEnvelope_withThreeDecimalPrices() throws Exception {
        ProductSearchResult result = new ProductSearchResult(
            List.of(new ProductSearchResult.Hit("MH-OUD-50", "Royal Oud 50ml", "MAISON", new BigDecimal("12.5"))), 37,
            List.of(new ProductSearchResult.Facet("MAISON", 21), new ProductSearchResult.Facet("ARABIAN", 16)),
            List.of(new ProductSearchResult.Facet("fragrance", 37)));
        when(searchService.search(eq("oud"), eq(List.of("MAISON", "ARABIAN")), any(), any(), any(), any(), any(), any()))
            .thenReturn(new SearchView(result, 0, 20, 2));

        mvc.perform(get("/api/v1/search/products").param("q", "oud").param("brand", "MAISON", "ARABIAN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].sku").value("MH-OUD-50"))
            .andExpect(jsonPath("$.content[0].price").value("12.500"))
            .andExpect(jsonPath("$.totalElements").value(37))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.facets.brand[0].key").value("MAISON"))
            .andExpect(jsonPath("$.facets.brand[0].count").value(21))
            .andExpect(jsonPath("$.facets.category[0].key").value("fragrance"));
    }

    @Test
    void search_shouldReturn400_whenMinPriceIsNotANumber() throws Exception {
        mvc.perform(get("/api/v1/search/products").param("minPrice", "cheap"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
