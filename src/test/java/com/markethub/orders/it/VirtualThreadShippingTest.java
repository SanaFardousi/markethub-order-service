package com.markethub.orders.it;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
    "markethub.carriers.executor=virtual",
    "markethub.carriers.desert-express.delay=3s"})
class VirtualThreadShippingTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("carrierExecutor")
    ExecutorService carrierExecutor;

    @Test
    void carrierExecutor_shouldRunOnVirtualThreads_whenTheSettingIsVirtual() throws Exception {
        Boolean virtual = carrierExecutor.submit(() -> Thread.currentThread().isVirtual()).get(5, TimeUnit.SECONDS);

        assertThat(virtual).isTrue();
    }

    @Test
    @DisplayName("with virtual threads and one 3-second carrier the endpoint still answers in under a second")
    void options_shouldAnswerInUnderOneSecond_withOneSlowCarrier() throws Exception {
        mvc.perform(get("/api/v1/shipping/options").param("country", "KW")).andExpect(status().isOk());   // warm-up

        long start = System.nanoTime();
        String json = mvc.perform(get("/api/v1/shipping/options").param("country", "KW"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertAll(
            () -> assertThat(elapsed).isLessThan(Duration.ofSeconds(1)),
            () -> assertThat(JsonPath.<List<String>>read(json, "$[*].carrier")).containsExactly("GulfPost", "FastCourier"));
    }
}
