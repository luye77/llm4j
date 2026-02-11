package com.bobo.llm4j.rag.vectorstore;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.List;

/**
 * Vector store abstraction.
 */
public interface VectorStore {

    void add(List<RagDocument> documents, List<List<Double>> vectors);

    List<RagDocument> similaritySearch(SearchRequest request, List<Double> queryEmbedding);

    void deleteByMetadata(String key, Object value);
}

