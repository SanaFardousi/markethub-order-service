package com.markethub.orders.it;

import com.markethub.orders.infrastructure.kafka.Topics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import java.util.UUID;
import com.markethub.orders.it.KafkaTestSupport.Collector;

import static com.markethub.orders.it.KafkaTestSupport.eventTypesFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderEventsTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("placing an order publishes OrderPlaced then OrderConfirmed, keyed by the order number")
    void placeOrder_shouldPublishOrderPlacedThenOrderConfirmed() throws Exception {
        long customer = customerId("customer01@example.com");

        try (Collector collector = new Collector(Topics.ORDERS)) {
            String number = placeCardOrder(customer);

            await().atMost(Duration.ofSeconds(30)).pollInSameThread().untilAsserted(() -> {
                List<ConsumerRecord<String, String>> all = collector.poll();
                assertThat(eventTypesFor(all, number)).containsExactly("OrderPlaced", "OrderConfirmed");

                String first = all.stream().filter(r -> number.equals(r.key())).findFirst().orElseThrow().value();
                assertAll(
                    () -> assertThat(JsonPath.<String>read(first, "$.aggregateId")).isEqualTo(number),
                    () -> assertThat(JsonPath.<String>read(first, "$.eventId")).hasSize(36),
                    () -> assertThat(JsonPath.<String>read(first, "$.occurredAt")).endsWith("Z"),
                    () -> assertThat(JsonPath.<String>read(first, "$.payload.total")).isEqualTo("31.350"),
                    () -> assertThat(JsonPath.<String>read(first, "$.payload.paymentMethod")).isEqualTo("CARD"));
            });
        }
    }

    @Test
    void placeOrder_shouldStoreOutboxRowsInTheSameTransaction_andNoneWhenPaymentIsDeclined() throws Exception {
        long customer = customerId("customer01@example.com");
        String declinedBody = """
                {"customerId": %d, "paymentMethod": "CARD",
                 "card": {"number": "4111111111110002", "expiry": "12/28"},
                 "shippingAddress": {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"},
                 "lines": [{"sku": "MH-OUD-50", "quantity": 1}]}""".formatted(customer);

        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(declinedBody))
            .andExpect(status().isUnprocessableEntity());

        assertThat(countOf("outbox_event")).isZero();
    }

    @Test
    void upsertProduct_shouldPublishProductUpsertedCarryingTheVersion() throws Exception {
        try (Collector collector = new Collector(Topics.PRODUCTS)) {
            mvc.perform(put("/api/v1/admin/products/TEST-EVENT-1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"nameEn": "Test", "nameAr": "test", "brand": "MAISON", "category": "fragrance",
                             "price": 12.500, "status": "ACTIVE"}"""))
                .andExpect(status().isOk());

            await().atMost(Duration.ofSeconds(30)).pollInSameThread().untilAsserted(() ->
                assertThat(collector.poll()).anySatisfy(record -> {
                    assertThat(record.key()).isEqualTo("TEST-EVENT-1");
                    assertThat(JsonPath.<String>read(record.value(), "$.type")).isEqualTo("ProductUpserted");
                    assertThat(JsonPath.<String>read(record.value(), "$.payload.price")).isEqualTo("12.500");
                    assertThat(JsonPath.<Integer>read(record.value(), "$.payload.version")).isEqualTo(0);
                }));
        }
    }

    @Test
    @DisplayName("each of 20 orders delivers its four events in the order they happened")
    void events_shouldArriveInOrder_whenTwentyOrdersAreShippedAndDelivered() throws Exception {
        long customer = customerId("customer01@example.com");
        List<String> expected = List.of("OrderPlaced", "OrderConfirmed", "OrderShipped", "OrderDelivered");

        try (Collector collector = new Collector(Topics.ORDERS)) {
            List<String> numbers = new ArrayList<>();
            for (int i = 0; i < 20; i++) numbers.add(placeCardOrder(customer));
            for (String number : numbers) transition(number, "SHIP");
            for (String number : numbers) transition(number, "DELIVER");

            await().atMost(Duration.ofSeconds(60)).pollInSameThread().untilAsserted(() -> {
                List<ConsumerRecord<String, String>> all = collector.poll();
                for (String number : numbers) {
                    assertThat(eventTypesFor(all, number)).as("events of %s", number).isEqualTo(expected);
                }
            });

            List<ConsumerRecord<String, String>> all = collector.poll();
            for (String number : numbers) {
                long partitions = all.stream().filter(r -> number.equals(r.key()))
                    .map(ConsumerRecord::partition).distinct().count();
                assertThat(partitions).as("partitions used by %s", number).isEqualTo(1);
            }
        }
    }
}
