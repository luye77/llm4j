package com.bobo.llm4j.rag;

import com.bobo.llm4j.platform.openai.embedding.entity.Embedding;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import com.bobo.llm4j.service.EmbeddingModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EmbeddingClient - 统一向量化客户端
 */
public class EmbeddingClient {
    private final EmbeddingModel embeddingModel;
    private final String model;

    public EmbeddingClient(EmbeddingModel embeddingModel, String model) {
        this.embeddingModel = embeddingModel;
        this.model = model;
    }

    public List<Float> embed(String text) throws Exception {
        List<List<Float>> results = embedAll(Collections.singletonList(text));
        return results.isEmpty() ? Collections.emptyList() : results.get(0);
    }

    public List<List<Float>> embedAll(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(texts)
                .model(model)
                .build();

        EmbeddingResponse response = embeddingModel.call(request);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }

        List<List<Float>> vectors = new ArrayList<>();
        for (Embedding embedding : response.getData()) {
            vectors.add(embedding.getEmbedding());
        }
        return vectors;
    }
}
