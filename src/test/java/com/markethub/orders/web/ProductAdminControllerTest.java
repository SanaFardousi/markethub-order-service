package com.markethub.orders.web;

import com.markethub.orders.application.ProductCommand;
import com.markethub.orders.application.ProductService;
import com.markethub.orders.application.ProductView;
import com.markethub.orders.domain.model.ProductStatus;
import com.markethub.orders.web.mapper.ApiMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductAdminController.class)
@Import({ApiMapperImpl.class, FixedClockConfig.class})
class ProductAdminControllerTest {

    private static final String VALID = """
            {"nameEn": "Royal Oud 50ml", "nameAr": "oud", "brand": "MAISON", "category": "fragrance",
             "price": 12.500, "status": "ACTIVE"}""";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    ProductService productService;

    @Test
    void upsert_shouldReturn200WithTheNewVersion_whenBodyIsValid() throws Exception {
        when(productService.upsert(any(ProductCommand.class))).thenReturn(new ProductView("MH-OUD-50",
            "Royal Oud 50ml", "oud", "MAISON", "fragrance", new BigDecimal("12.500"), "KWD",
            ProductStatus.ACTIVE, 3L));

        mvc.perform(put("/api/v1/admin/products/MH-OUD-50").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("MH-OUD-50"))
            .andExpect(jsonPath("$.price").value("12.500"))
            .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void upsert_shouldReturn400_whenPriceIsNegative() throws Exception {
        mvc.perform(put("/api/v1/admin/products/MH-OUD-50").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("12.500", "-1")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')]").exists());
    }

    @Test
    void upsert_shouldReturn400_whenNameIsMissing() throws Exception {
        mvc.perform(put("/api/v1/admin/products/MH-OUD-50").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("\"nameEn\": \"Royal Oud 50ml\",", "")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == 'nameEn')]").exists());
    }
}
