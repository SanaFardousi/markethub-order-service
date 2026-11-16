package com.markethub.orders.config;

import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

/**
 * Points the Elasticsearch client at {@code markethub.search.url}.
 * Spring Boot builds its Elasticsearch client from an {@link ElasticsearchConnectionDetails} bean when one
 * exists, so this setting replaces {@code spring.elasticsearch.uris} and is the only place the address is set.
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    ElasticsearchConnectionDetails elasticsearchConnectionDetails(AppProperties properties) {
        Node node = toNode(properties.search().url());
        return () -> List.of(node);
    }

    static Node toNode(String url) {
        URI uri = URI.create(url);
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException(
                "markethub.search.url must look like http://host:9200, but was: " + url);
        }
        Protocol protocol = "https".equals(uri.getScheme()) ? Protocol.HTTPS : Protocol.HTTP;
        int port = uri.getPort() != -1 ? uri.getPort() : (protocol == Protocol.HTTPS ? 443 : 80);
        return new Node(uri.getHost(), port, protocol);
    }
}
