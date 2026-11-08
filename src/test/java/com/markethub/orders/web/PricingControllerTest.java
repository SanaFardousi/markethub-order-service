package com.markethub.orders.web;

import com.markethub.orders.application.PricingService;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.pricing.AppliedPromotion;
import com.markethub.orders.domain.pricing.PricingResult;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PricingController.class)
@Import(FixedClockConfig.class)
class PricingControllerTest {

    private static final String BODY = """
            {"customerId": 1, "couponCode": "WELCOME10", "paymentMethod": "CARD",
             "lines": [{"sku": "MH-OUD-50", "quantity": 2}]}""";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    PricingService pricingService;

    @Test
    void preview_shouldReturn200WithThreeDecimalStrings_whenRequestIsValid() throws Exception {
        PricingResult result = new PricingResult(new BigDecimal("33.000"), new BigDecimal("3.300"),
            new BigDecimal("0.000"), new BigDecimal("29.700"),
            List.of(new AppliedPromotion("R2", "WELCOME10 coupon", new BigDecimal("3.300"))), false);
        when(pricingService.price(eq(1L), eq("WELCOME10"), eq(PaymentMethod.CARD), anyList())).thenReturn(result);

        mvc.perform(post("/api/v1/pricing/preview").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("KWD"))
            .andExpect(jsonPath("$.subtotal").value("33.000"))
            .andExpect(jsonPath("$.shipping").value("0.000"))
            .andExpect(jsonPath("$.total").value("29.700"))
            .andExpect(jsonPath("$.promotions[0].amount").value("3.300"))
            .andExpect(jsonPath("$.onHold").value(false));
    }

    @Test
    void preview_shouldReturn400_whenLinesAreMissing() throws Exception {
        mvc.perform(post("/api/v1/pricing/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\": 1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void preview_shouldReturn404_whenCustomerDoesNotExist() throws Exception {
        when(pricingService.price(any(), any(), any(), anyList())).thenThrow(new NotFoundException("no customer"));

        mvc.perform(post("/api/v1/pricing/preview").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
