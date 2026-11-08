package com.markethub.orders.it;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderFlowTest extends AbstractIntegrationTest {

    private static final String GOOD_CARD = "4111111111111111";
    private static final String DECLINED_CARD = "4111111111110002";
    private static final String LINES = """
            [{"sku": "MH-OUD-50", "quantity": 2}, {"sku": "MH-ROSE-30", "quantity": 1}]""";

    private static String orderJson(long customerId, String coupon, String method, String card, String lines) {
        return """
                {"customerId": %d, "couponCode": %s, "paymentMethod": "%s", "card": %s,
                 "shippingAddress": {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"},
                 "lines": %s}
                """.formatted(customerId,
            coupon == null ? "null" : "\"" + coupon + "\"", method,
            card == null ? "null" : "{\"number\": \"" + card + "\", \"expiry\": \"12/28\"}", lines);
    }

    private ResultActions place(String key, String json) throws Exception {
        return mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String orderNumberOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.orderNumber");
    }

    private static String newKey() { return UUID.randomUUID().toString(); }

    @Test
    @DisplayName("the welcome coupon's effect is stored in the database")
    void placeOrder_shouldStoreDiscountedTotalsAndPayment_whenWelcomeCouponApplies() throws Exception {
        long customer = customerId("customer01@example.com");

        place(newKey(), orderJson(customer, "WELCOME10", "CARD", GOOD_CARD, LINES))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.total").value("29.700"));

        Map<String, Object> order = jdbc.queryForMap("select status, subtotal, discount, shipping, total from orders");
        Map<String, Object> payment = jdbc.queryForMap("select method, status, card_last4, amount from payment");
        String promotionId = jdbc.queryForObject(
            "select json_unquote(json_extract(promotions, '$[0].id')) from orders", String.class);
        assertAll(
            () -> assertThat(order.get("status")).isEqualTo("CONFIRMED"),
            () -> assertThat((BigDecimal) order.get("subtotal")).isEqualByComparingTo("33.000"),
            () -> assertThat((BigDecimal) order.get("discount")).isEqualByComparingTo("3.300"),
            () -> assertThat((BigDecimal) order.get("shipping")).isEqualByComparingTo("0.000"),
            () -> assertThat((BigDecimal) order.get("total")).isEqualByComparingTo("29.700"),
            () -> assertThat(promotionId).isEqualTo("R2"),
            () -> assertThat(payment.get("method")).isEqualTo("CARD"),
            () -> assertThat(payment.get("status")).isEqualTo("AUTHORISED"),
            () -> assertThat(payment.get("card_last4")).isEqualTo("1111"),
            () -> assertThat(countOf("order_line")).isEqualTo(2));
    }

    @Test
    void placeOrder_shouldCreateOnlyOneOrder_whenTheSameKeyAndBodyAreSentTwice() throws Exception {
        long customer = customerId("customer01@example.com");
        String key = newKey();
        String json = orderJson(customer, "WELCOME10", "CARD", GOOD_CARD, LINES);

        String first = orderNumberOf(place(key, json).andExpect(status().isCreated()));
        String second = orderNumberOf(place(key, json).andExpect(status().isCreated()));

        assertAll(
            () -> assertThat(second).isEqualTo(first),
            () -> assertThat(countOf("orders")).isEqualTo(1),
            () -> assertThat(countOf("payment")).isEqualTo(1));
    }

    @Test
    void placeOrder_shouldReturn409_whenTheSameKeyIsReusedWithADifferentBody() throws Exception {
        long customer = customerId("customer01@example.com");
        String key = newKey();
        place(key, orderJson(customer, null, "CARD", GOOD_CARD, LINES)).andExpect(status().isCreated());

        place(key, orderJson(customer, null, "CARD", GOOD_CARD, LINES.replace("\"quantity\": 2", "\"quantity\": 3")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        assertThat(countOf("orders")).isEqualTo(1);
    }

    @Test
    @DisplayName("a declined card leaves no order, no lines and no payment behind")
    void placeOrder_shouldReturn422AndLeaveNothingBehind_whenCardIsDeclined() throws Exception {
        long customer = customerId("customer01@example.com");

        place(newKey(), orderJson(customer, null, "CARD", DECLINED_CARD, LINES))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PAYMENT_DECLINED"));

        assertAll(
            () -> assertThat(countOf("orders")).isZero(),
            () -> assertThat(countOf("order_line")).isZero(),
            () -> assertThat(countOf("payment")).isZero());
    }

    @Test
    void placeOrder_shouldReturn409AndCreateNothing_whenSkuIsInactive() throws Exception {
        mvc.perform(put("/api/v1/admin/products/TEST-INACTIVE").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nameEn": "Test", "nameAr": "test", "brand": "MAISON", "category": "fragrance",
                         "price": 1.000, "status": "INACTIVE"}"""))
            .andExpect(status().isOk());
        long customer = customerId("customer01@example.com");

        place(newKey(), orderJson(customer, null, "CARD", GOOD_CARD, "[{\"sku\": \"TEST-INACTIVE\", \"quantity\": 1}]"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));

        assertThat(countOf("orders")).isZero();
    }

    @Test
    void transitions_shouldShipOnceAndRefuseEverythingAfterwards() throws Exception {
        long customer = customerId("customer01@example.com");
        String number = orderNumberOf(place(newKey(), orderJson(customer, null, "CARD", GOOD_CARD, LINES))
            .andExpect(status().isCreated()));

        postJson("/api/v1/orders/" + number + "/transitions", "{\"action\": \"SHIP\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHIPPED"));
        postJson("/api/v1/orders/" + number + "/transitions", "{\"action\": \"SHIP\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
        postJson("/api/v1/orders/" + number + "/cancel", "{\"reason\": \"too late\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));

        assertThat(jdbc.queryForObject("select status from orders", String.class)).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("cancelling a paid order refunds the payment in the database")
    void cancel_shouldRefundPayment_whenCardOrderIsCancelled() throws Exception {
        long customer = customerId("customer01@example.com");
        String number = orderNumberOf(place(newKey(), orderJson(customer, null, "CARD", GOOD_CARD, LINES))
            .andExpect(status().isCreated()));

        postJson("/api/v1/orders/" + number + "/cancel", "{\"reason\": \"changed my mind\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertAll(
            () -> assertThat(jdbc.queryForObject("select status from orders", String.class)).isEqualTo("CANCELLED"),
            () -> assertThat(jdbc.queryForObject("select status from payment", String.class)).isEqualTo("REFUNDED"));
    }

    @Test
    @DisplayName("a large cash-on-delivery order is held, then approved")
    void placeOrder_shouldHoldLargeCashOnDeliveryOrder_untilItIsApproved() throws Exception {
        long customer = customerId("customer02@example.com");
        String number = orderNumberOf(place(newKey(),
            orderJson(customer, null, "COD", null, "[{\"sku\": \"MH-OUD-100\", \"quantity\": 8}]"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ON_HOLD"))
            .andExpect(jsonPath("$.total").value("154.800")));

        postJson("/api/v1/orders/" + number + "/transitions", "{\"action\": \"APPROVE\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertAll(
            () -> assertThat(jdbc.queryForObject("select status from orders", String.class)).isEqualTo("CONFIRMED"),
            () -> assertThat(jdbc.queryForObject("select status from payment", String.class)).isEqualTo("PENDING"));
    }
}
