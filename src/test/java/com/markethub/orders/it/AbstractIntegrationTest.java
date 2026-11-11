package com.markethub.orders.it;

import com.jayway.jsonpath.JsonPath;
import com.markethub.orders.SharedElasticsearch;
import com.markethub.orders.SharedKafka;
import com.markethub.orders.SharedMySql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySql.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySql.INSTANCE::getPassword);
        registry.add("markethub.search.url", SharedElasticsearch::url);
        registry.add("spring.elasticsearch.uris", SharedElasticsearch::url);
    }

    @BeforeEach
    void cleanBefore() { cleanDatabase(); }

    @AfterEach
    void cleanAfter() { cleanDatabase(); }

    protected long customerId(String email) {
        return jdbc.queryForObject("select id from customer where email = ?", Long.class, email);
    }

    protected long countOf(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySql.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySql.INSTANCE::getPassword);
        registry.add("spring.kafka.bootstrap-servers", SharedKafka.INSTANCE::getBootstrapServers);
    }

    private void cleanDatabase() {
        jdbc.update("delete from outbox_event");
        jdbc.update("delete from payment");
        jdbc.update("delete from order_line");
        jdbc.update("delete from orders");
        jdbc.update("delete from product where sku like 'TEST-%'");
    }

    protected String placeCardOrder(long customerId) throws Exception {
        String body = """
            {"customerId": %d, "paymentMethod": "CARD",
             "card": {"number": "4111111111111111", "expiry": "12/28"},
             "shippingAddress": {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"},
             "lines": [{"sku": "MH-OUD-50", "quantity": 2}, {"sku": "MH-ROSE-30", "quantity": 1}]}
            """.formatted(customerId);
        String response = mvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.orderNumber");
    }

    protected void transition(String orderNumber, String action) throws Exception {
        mvc.perform(post("/api/v1/orders/" + orderNumber + "/transitions")
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"" + action + "\"}"))
            .andExpect(status().isOk());
    }
}
