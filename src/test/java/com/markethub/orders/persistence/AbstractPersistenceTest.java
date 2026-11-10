package com.markethub.orders.persistence;

import com.markethub.orders.SharedMySql;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")

abstract class AbstractPersistenceTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedMySql.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", SharedMySql.INSTANCE::getUsername);
        registry.add("spring.datasource.password", SharedMySql.INSTANCE::getPassword);
    }
}
