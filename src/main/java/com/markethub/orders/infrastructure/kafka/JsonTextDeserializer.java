package com.markethub.orders.infrastructure.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

public class JsonTextDeserializer implements Deserializer<String> {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        String text = new String(data, StandardCharsets.UTF_8);
        JSON.readTree(text);          // throws if the text is not valid JSON
        return text;
    }
}
