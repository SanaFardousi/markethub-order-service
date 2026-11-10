package com.markethub.orders.it;

import com.jayway.jsonpath.JsonPath;
import com.markethub.orders.SharedKafka;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class KafkaTestSupport {

    private KafkaTestSupport() {}

    /** Sends one message with a plain Kafka producer, completely separate from the application. */
    static void send(String topic, String key, String value) {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafka.INSTANCE.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not send test message", e);
        }
    }

    /** A throwaway consumer that reads a topic from the beginning and remembers what it saw. */
    static final class Collector implements AutoCloseable {

        private final KafkaConsumer<String, String> consumer;
        private final List<ConsumerRecord<String, String>> records = new ArrayList<>();

        Collector(String... topics) {
            Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafka.INSTANCE.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of(topics));
        }

        /** Reads whatever has arrived since the last call, and returns everything seen so far. */
        List<ConsumerRecord<String, String>> poll() {
            consumer.poll(Duration.ofMillis(200)).forEach(records::add);
            return records;
        }

        @Override
        public void close() { consumer.close(); }
    }

    static List<String> eventTypesFor(List<ConsumerRecord<String, String>> records, String key) {
        return records.stream()
            .filter(r -> key.equals(r.key()))
            .map(r -> JsonPath.<String>read(r.value(), "$.type"))
            .toList();
    }
}
