package com.markethub.orders.it;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityTest extends AbstractIntegrationTest {

    @Test
    void health_shouldBeUpAndIncludeDatabaseKafkaAndElasticsearch() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.db.status").value("UP"))
            .andExpect(jsonPath("$.components.kafka.status").value("UP"))
            .andExpect(jsonPath("$.components.elasticsearch.status").value("UP"));
    }

    @Test
    @DisplayName("custom metrics are exported, and no tag holds an order number or SKU")
    void prometheus_shouldShowCustomMetricsWithoutHighCardinalityTags() throws Exception {
        String orderNumber = placeCardOrder(customerId("customer01@example.com"));

        String metrics = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertAll(
            () -> assertThat(metrics).contains("markethub_orders_placed_total"),
            () -> assertThat(metrics).contains("method=\"CARD\"").contains("outcome=\"success\""),
            () -> assertThat(metrics).contains("markethub_pricing_duration_seconds_count"),
            () -> assertThat(metrics).contains("markethub_outbox_unpublished"),
            () -> assertThat(metrics).doesNotContain(orderNumber).doesNotContain("MH-OUD-50"));
    }

    @Test
    void errors_shouldCarryTheRequestIdFromTheHeader() throws Exception {
        mvc.perform(get("/api/v1/orders/NOPE").header("X-Request-Id", "trace-123"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-Request-Id", "trace-123"))
            .andExpect(jsonPath("$.traceId").value("trace-123"));
    }

    @Test
    @DisplayName("the OpenAPI document lists every endpoint with its error responses")
    void apiDocs_shouldListEveryEndpointAndTheErrorSchema() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Set<String> paths = JsonPath.<Map<String, Object>>read(json, "$.paths").keySet();
        assertAll(
            () -> assertThat(paths).contains("/api/v1/pricing/preview", "/api/v1/orders",
                "/api/v1/orders/{orderNumber}", "/api/v1/customers/{id}/orders",
                "/api/v1/orders/{orderNumber}/cancel", "/api/v1/orders/{orderNumber}/transitions",
                "/api/v1/admin/products/{sku}", "/api/v1/search/products", "/api/v1/shipping/options"),
            () -> assertThat(JsonPath.<Object>read(json, "$.paths['/api/v1/orders'].post.responses['422']")).isNotNull(),
            () -> assertThat(JsonPath.<Object>read(json, "$.paths['/api/v1/orders'].post.responses['400']")).isNotNull(),
            () -> assertThat(JsonPath.<Object>read(json, "$.paths['/api/v1/shipping/options'].get.responses['503']")).isNotNull(),
            () -> assertThat(JsonPath.<Object>read(json, "$.components.schemas.ApiError")).isNotNull());
    }
}
