package com.bobo.llm4j.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.bobo.llm4j.platform.openai.chat.entity.Usage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * EmbeddingResponse - Embedding响应实体 (对应Spring AI的EmbeddingResponse)
 */
@Data
@NoArgsConstructor()
@AllArgsConstructor()
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingResponse {
    private String object;
    private List<Embedding> data;
    private String model;
    private Usage usage;
}
