package com.markethub.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "markethub")
public record AppProperties(@DefaultValue Search search) {

    public record Search(@DefaultValue("http://localhost:9200") String url) {}
}
