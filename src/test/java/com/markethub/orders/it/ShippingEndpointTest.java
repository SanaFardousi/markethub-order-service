package com.markethub.orders.it;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "markethub.carriers.desert-express.delay=3s")
class ShippingEndpointTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("with one carrier at 3 seconds the endpoint still answers in under a second")
    void options_shouldAnswerInUnderOneSecondWithTheTwoFastCarriers() throws Exception {
        mvc.perform(get("/api/v1/shipping/options").param("country", "KW")).andExpect(status().isOk());   // warm-up

        long start = System.nanoTime();
        String json = mvc.perform(get("/api/v1/shipping/options").param("country", "KW"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertAll(
            () -> assertThat(elapsed).isLessThan(Duration.ofSeconds(1)),
            () -> assertThat(JsonPath.<List<String>>read(json, "$[*].carrier")).containsExactly("GulfPost", "FastCourier"),
            () -> assertThat(JsonPath.<String>read(json, "$[0].price")).isEqualTo("1.500"));
    }
}
