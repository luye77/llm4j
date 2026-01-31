package com.bobo.llm4j.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * EmbeddingRequest - Embedding请求实体 (对应Spring AI的EmbeddingRequest)
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingRequest {
    /**
     * 向量化文本
     */
    @NonNull
    private Object input;

    /**
     * 向量模型
     */
    @NonNull
    @Builder.Default
    private String model = "text-embedding-3-small";

    @JsonProperty("encoding_format")
    private String encodingFormat;

    /**
     * 向量维度
     */
    private String dimensions;

    private String user;

    public static class EmbeddingRequestBuilder {
        private Object input;

        private EmbeddingRequest.EmbeddingRequestBuilder input(Object input){
            this.input = input;
            return this;
        }

        public EmbeddingRequest.EmbeddingRequestBuilder input(String input){
            this.input = input;
            return this;
        }

        public EmbeddingRequest.EmbeddingRequestBuilder input(List<String> content){
            this.input = content;
            return this;
        }
    }
}

