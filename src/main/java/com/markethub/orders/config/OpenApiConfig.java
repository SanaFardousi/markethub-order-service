package com.markethub.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI markethubOpenApi() {
        return new OpenAPI().info(new Info()
            .title("MarketHub Order & Search Service")
            .version("v1")
            .description("Prices baskets with Drools rules, takes orders, publishes events to Kafka and serves "
                + "product search. Every error uses one body shape, and clients branch on its `code`."));
    }
}
