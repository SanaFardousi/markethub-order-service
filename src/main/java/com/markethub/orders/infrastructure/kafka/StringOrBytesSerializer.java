package com.markethub.orders.infrastructure.kafka;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class StringOrBytesSerializer implements Serializer<Object> {

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof byte[] bytes) {
            return bytes;
        }
        return data.toString().getBytes(StandardCharsets.UTF_8);
    }
}
