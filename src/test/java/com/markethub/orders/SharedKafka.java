package com.markethub.orders;

import com.markethub.orders.infrastructure.kafka.Topics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SharedKafka {

    public static final KafkaContainer INSTANCE = new KafkaContainer("apache/kafka:3.9.2");

    static {
        INSTANCE.start();
        createTopics();
    }

    private SharedKafka() {}

    private static void createTopics() {
        try (AdminClient admin = AdminClient.create(
            Map.<String, Object>of("bootstrap.servers", INSTANCE.getBootstrapServers()))) {
            admin.createTopics(List.of(
                new NewTopic(Topics.ORDERS, 3, (short) 1),
                new NewTopic(Topics.PRODUCTS, 3, (short) 1),
                new NewTopic(Topics.PRODUCTS_DLT, 3, (short) 1))).all().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create test topics", e);
        }
    }
}
