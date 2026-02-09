package com.bobo.llm4j.chat.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.bobo.llm4j.chat.entity.Media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MediaDeserializer - Media内容反序列化器
 */
public class MediaDeserializer extends JsonDeserializer<Media> {
    @Override
    public Media deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            return Media.ofText(node.asText());
        } else if (node.isArray()) {
            List<Media.MultiModal> parts = new ArrayList<>();
            for (JsonNode element : node) {
                Media.MultiModal part = p.getCodec().treeToValue(element, Media.MultiModal.class);
                parts.add(part);
            }
            return Media.ofMultiModals(parts);
        }
        throw new IOException("Unsupported media format");
    }
}

