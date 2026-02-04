package com.bobo.llm4j.rag;

import java.util.List;

/**
 * SimpleRetriever - 基于向量的检索实现
 */
public class SimpleRetriever implements Retriever {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public SimpleRetriever(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    @Override
    public List<ScoredDocument> retrieve(String query, int topK) throws Exception {
        List<Float> vector = embeddingClient.embed(query);
        return vectorStore.query(vector, topK);
    }
}
