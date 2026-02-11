package com.bobo.llm4j.rag.embedding;

import java.util.Collections;
import java.util.List;

/**
 * Embedding model abstraction.
 */
public interface EmbeddingModel {

    List<List<Double>> embedAll(List<String> texts) throws Exception;

    default List<Double> embed(String text) throws Exception {
        List<List<Double>> vectors = embedAll(Collections.singletonList(text));
        return vectors.isEmpty() ? Collections.<Double>emptyList() : vectors.get(0);
    }
}

