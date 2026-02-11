package com.bobo.llm4j.rag.retrieval;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.List;
import java.util.Map;

/**
 * Retrieval abstraction.
 */
public interface DocumentRetriever {

    List<RagDocument> retrieve(String query, Map<String, Object> runtimeFilters) throws Exception;
}

