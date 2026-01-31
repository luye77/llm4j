package com.bobo.llm4j.service;

import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;

/**
 * EmbeddingModel - Embedding模型接口 (对应Spring AI的EmbeddingModel)
 */
public interface EmbeddingModel {

    EmbeddingResponse call(String baseUrl, String apiKey, EmbeddingRequest request) throws Exception;
    EmbeddingResponse call(EmbeddingRequest request) throws Exception;
}
