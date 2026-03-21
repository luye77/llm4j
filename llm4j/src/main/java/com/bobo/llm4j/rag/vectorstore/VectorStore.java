package com.bobo.llm4j.rag.vectorstore;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.etl.DocumentWriter;

import java.util.List;

/**
 * Vector store abstraction that doubles as a {@link DocumentWriter}.
 * Implementations embed documents internally via their {@link com.bobo.llm4j.rag.embedding.EmbeddingModel}.
 */
public interface VectorStore extends DocumentWriter {

    void add(List<RagDocument> documents);

    @Override
    default void write(List<RagDocument> documents) {
        add(documents);
    }

    List<RagDocument> similaritySearch(SearchRequest request);

    void deleteByMetadata(String key, Object value);
}
