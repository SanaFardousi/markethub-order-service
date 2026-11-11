package com.markethub.orders.web;

import com.markethub.orders.application.ShippingOption;
import com.markethub.orders.application.ShippingService;
import com.markethub.orders.domain.exception.NoCarrierAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShippingController.class)
@Import(FixedClockConfig.class)
class ShippingControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    ShippingService shippingService;

    @Test
    void options_shouldReturnCarriersWithThreeDecimalPrices() throws Exception {
        when(shippingService.options("KW"))
            .thenReturn(List.of(new ShippingOption("GulfPost", new BigDecimal("1.5"), 4)));

        mvc.perform(get("/api/v1/shipping/options").param("country", "KW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].carrier").value("GulfPost"))
            .andExpect(jsonPath("$[0].price").value("1.500"))
            .andExpect(jsonPath("$[0].currency").value("KWD"))
            .andExpect(jsonPath("$[0].estimatedDays").value(4));
    }

    @Test
    void options_shouldReturn503_whenNoCarrierAnswered() throws Exception {
        when(shippingService.options("KW")).thenThrow(new NoCarrierAvailableException());

        mvc.perform(get("/api/v1/shipping/options").param("country", "KW"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("NO_CARRIER_AVAILABLE"));
    }

    @Test
    void options_shouldReturn400_whenCountryIsMissing() throws Exception {
        mvc.perform(get("/api/v1/shipping/options"))
            .andExpect(status().isBadRequest());
    }
}
