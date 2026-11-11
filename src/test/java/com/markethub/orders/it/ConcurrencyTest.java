package com.markethub.orders.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ConcurrencyTest extends AbstractIntegrationTest {

    private static final int THREADS = 20;

    /**
     * Parks every action on its own thread, then releases them all at the same instant.
     */
    private <T> List<T> runTogether(List<Callable<T>> actions) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(actions.size());
        CountDownLatch ready = new CountDownLatch(actions.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> action : actions) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return action.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @RepeatedTest(10)
    @DisplayName("20 simultaneous requests with the same key create exactly one order")
    void placeOrder_shouldCreateOneOrder_when20RequestsShareAKey() throws Exception {
        long customer = customerId("customer01@example.com");
        String key = UUID.randomUUID().toString();
        String body = """
                {"customerId": %d, "paymentMethod": "CARD",
                 "card": {"number": "4111111111111111", "expiry": "12/28"},
                 "shippingAddress": {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"},
                 "lines": [{"sku": "MH-OUD-50", "quantity": 2}]}""".formatted(customer);

        List<Callable<String>> requests = Collections.nCopies(THREADS, () -> {
            String response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
            return JsonPath.<String>read(response, "$.orderNumber");
        });

        Set<String> orderNumbers = new HashSet<>(runTogether(requests));

        assertAll(
            () -> assertThat(orderNumbers).hasSize(1),
            () -> assertThat(jdbc.queryForObject("select count(*) from orders where idempotency_key = ?",
                Long.class, key)).isEqualTo(1),
            () -> assertThat(countOf("payment")).isEqualTo(1),          // charged once
            () -> assertThat(countOf("outbox_event")).isEqualTo(2));    // events written once
    }
}
