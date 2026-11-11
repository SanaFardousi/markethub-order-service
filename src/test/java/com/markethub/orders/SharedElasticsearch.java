package com.markethub.orders;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public final class SharedElasticsearch {

    public static final GenericContainer<?> INSTANCE =
        new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:9.0.3")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=60s")
                .forPort(9200).withStartupTimeout(Duration.ofMinutes(3)));

    static { INSTANCE.start(); }

    private SharedElasticsearch() {}

    public static String url() {
        return "http://" + INSTANCE.getHost() + ":" + INSTANCE.getMappedPort(9200);
    }
}
