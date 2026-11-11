//package com.markethub.orders.config;
//
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//import org.apache.http.HttpHost;
//import org.elasticsearch.client.RestClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ElasticsearchConfig {
//
//    @Bean
//    RestClient elasticsearchRestClient(@Value("${markethub.search.url}") String url) {
//        return RestClient.builder(HttpHost.create(url)).build();
//    }
//
//    @Bean
//    ElasticsearchClient elasticsearchClient(RestClient restClient) {
//        return new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
//    }
//}
