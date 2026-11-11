package com.markethub.orders.it;

import com.markethub.orders.infrastructure.kafka.Topics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import com.markethub.orders.it.KafkaTestSupport.Collector;
import org.springframework.kafka.support.KafkaHeaders;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.util.UUID;

@ExtendWith(OutputCaptureExtension.class)
class BrokenMessageTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("a broken message goes to the dead-letter topic and does not block the next one")
    void brokenMessage_shouldLandInTheDeadLetterTopic_andNotBlockLaterMessages(CapturedOutput output) {
        String sku = "TEST-DLT-" + UUID.randomUUID();       // one key means one partition, so the broken message really is "in front"
        String broken = "{ this is not json";
        String valid = """
        {"eventId": "%s", "type": "ProductUpserted", "occurredAt": "2026-11-01T10:00:00Z",
         "aggregateId": "%s",
         "payload": {"sku": "%s", "nameEn": "Test", "nameAr": "test", "brand": "MAISON", "category": "fragrance",
                     "price": "1.000", "currency": "KWD", "status": "ACTIVE", "version": 1}}"""
            .formatted(UUID.randomUUID(), sku, sku);

        try (Collector deadLetters = new Collector(Topics.PRODUCTS_DLT)) {
            KafkaTestSupport.send(Topics.PRODUCTS, sku, broken);
            KafkaTestSupport.send(Topics.PRODUCTS, sku, valid);

            await().atMost(Duration.ofSeconds(30)).pollInSameThread().untilAsserted(() -> {
                assertThat(deadLetters.poll()).anySatisfy(record -> {
                    assertThat(record.key()).isEqualTo(sku);
                    assertThat(record.value()).isEqualTo(broken);
                    assertThat(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC)).isNotNull();
                });
                assertThat(output.getOut()).contains("Received ProductUpserted for sku " + sku);
            });
        }
    }
}
