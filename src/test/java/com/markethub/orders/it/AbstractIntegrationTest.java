package com.markethub.orders.it;

import com.markethub.orders.SharedMySql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
    }

    @BeforeEach
    void cleanBefore() { cleanDatabase(); }

    @AfterEach
    void cleanAfter() { cleanDatabase(); }

    /** Children before parents, because of the foreign keys. Seed customers and products stay. */
    private void cleanDatabase() {
        jdbc.update("delete from payment");
        jdbc.update("delete from order_line");
        jdbc.update("delete from orders");
        jdbc.update("delete from product where sku like 'TEST-%'");
    }

    protected long customerId(String email) {
        return jdbc.queryForObject("select id from customer where email = ?", Long.class, email);
    }

    protected long countOf(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }
}
