package com.bobo.llm4j.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.bobo.llm4j.platform.openai.chat.serializer.MediaDeserializer;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Media - 媒体内容实体 (对应Spring AI的Media)
 */
@ToString
@JsonDeserialize(using = MediaDeserializer.class)
public class Media {
    private String text;
    private List<MultiModal> multiModals;

    public static Media ofText(String text) {
        Media instance = new Media();
        instance.text = text;
        return instance;
    }

    public static Media ofMultiModals(List<MultiModal> parts) {
        Media instance = new Media();
        instance.multiModals = parts;
        return instance;
    }

    @JsonValue
    public Object toJson() {
        if (text != null) {
            return text;
        } else if (multiModals != null) {
            return multiModals;
        }
        throw new IllegalStateException("Invalid media state");
    }

    public String getText() { return text; }
    public List<MultiModal> getMultiModals() { return multiModals; }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MultiModal {
        private String type = Type.TEXT.type;
        private String text;
        @JsonProperty("image_url")
        private ImageUrl imageUrl;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ImageUrl {
            private String url;
        }

        @Getter
        @AllArgsConstructor
        public enum Type {
            TEXT("text", "文本类型"),
            IMAGE_URL("image_url", "图片类型"),
            ;
            private final String type;
            private final String info;
        }

        public static List<MultiModal> withMultiModal(String text, String... imageUrl) {
            List<MultiModal> messages = new ArrayList<>();
            messages.add(new MultiModal(MultiModal.Type.TEXT.getType(), text, null));
            for (String url : imageUrl) {
                messages.add(new MultiModal(MultiModal.Type.IMAGE_URL.getType(), null, new MultiModal.ImageUrl(url)));
            }
            return messages;
        }
    }
}

