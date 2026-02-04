package com.bobo.llm4j.rag;

import java.util.List;

/**
 * Retriever - 检索接口
 */
public interface Retriever {
    List<ScoredDocument> retrieve(String query, int topK) throws Exception;
}
