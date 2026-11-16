package com.markethub.orders.unit;

import com.markethub.orders.config.AppProperties;
import com.markethub.orders.config.ElasticsearchConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @EnableConfigurationProperties(AppProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner =
        new ApplicationContextRunner().withUserConfiguration(Config.class, ElasticsearchConfig.class);

    @Test
    void search_shouldDefaultToLocalhost_whenNothingIsConfigured() {
        runner.run(context -> {
            assertThat(context.getBean(AppProperties.class).search().url()).isEqualTo("http://localhost:9200");
            assertThat(context.getBean(ElasticsearchConnectionDetails.class).getNodes())
                .containsExactly(new Node("localhost", 9200, Protocol.HTTP));
        });
    }

    @Test
    void search_shouldPointTheElasticsearchConnectionAtTheConfiguredUrl() {
        runner.withPropertyValues("markethub.search.url=https://search.internal:9243")
            .run(context -> assertThat(context.getBean(ElasticsearchConnectionDetails.class).getNodes())
                .containsExactly(new Node("search.internal", 9243, Protocol.HTTPS)));
    }

    @Test
    void search_shouldFailAtStartup_whenTheUrlIsNotAnHttpAddress() {
        runner.withPropertyValues("markethub.search.url=elasticsearch:9200")
            .run(context -> assertThat(context).getFailure()
                .rootCause().hasMessageContaining("markethub.search.url must look like http://host:9200"));
    }
}
