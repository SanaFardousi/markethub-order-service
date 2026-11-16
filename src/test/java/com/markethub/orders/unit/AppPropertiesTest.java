package com.markethub.orders.unit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.markethub.orders.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @EnableConfigurationProperties(AppProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void search_shouldDefaultToLocalhost_whenNothingIsConfigured() {
        runner.run(context -> assertThat(context.getBean(AppProperties.class).search().url())
                .isEqualTo("http://localhost:9200"));
    }

    @Test
    void search_shouldUseTheConfiguredUrl() {
        runner.withPropertyValues("markethub.search.url=http://elasticsearch:9200")
                .run(context -> assertThat(context.getBean(AppProperties.class).search().url())
                        .isEqualTo("http://elasticsearch:9200"));
    }
}
