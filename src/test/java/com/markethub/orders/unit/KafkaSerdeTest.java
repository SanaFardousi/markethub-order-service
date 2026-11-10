package com.markethub.orders.unit;

import com.markethub.orders.infrastructure.kafka.JsonTextDeserializer;
import com.markethub.orders.infrastructure.kafka.StringOrBytesSerializer;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaSerdeTest {

    private final JsonTextDeserializer deserializer = new JsonTextDeserializer();
    private final StringOrBytesSerializer serializer = new StringOrBytesSerializer();

    @Test
    void deserialize_shouldReturnTheText_whenItIsValidJson() {
        assertThat(deserializer.deserialize("t", "{\"a\": 1}".getBytes(UTF_8))).isEqualTo("{\"a\": 1}");
    }

    @Test
    void deserialize_shouldThrow_whenTheTextIsNotJson() {
        assertThatThrownBy(() -> deserializer.deserialize("t", "{ not json".getBytes(UTF_8)))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deserialize_shouldReturnNull_whenThereIsNoValue() {
        assertThat(deserializer.deserialize("t", null)).isNull();
    }

    @Test
    void serialize_shouldPassBytesThroughUnchanged() {
        byte[] raw = {1, 2, 3};
        assertThat(serializer.serialize("t", raw)).isSameAs(raw);
    }

    @Test
    void serialize_shouldWriteTextAsUtf8() {
        assertThat(serializer.serialize("t", "عود")).isEqualTo("عود".getBytes(UTF_8));
    }

    @Test
    void serialize_shouldReturnNull_whenThereIsNoValue() {
        assertThat(serializer.serialize("t", null)).isNull();
    }
}
