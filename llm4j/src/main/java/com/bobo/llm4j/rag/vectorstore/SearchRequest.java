package com.bobo.llm4j.rag.vectorstore;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Search request for vector retrieval.
 */
@Data
@Builder(toBuilder = true)
public class SearchRequest {

    public static final double SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0d;
    public static final int DEFAULT_TOP_K = 4;

    @Builder.Default
    private String query = "";

    @Builder.Default
    private int topK = DEFAULT_TOP_K;

    @Builder.Default
    private double similarityThreshold = SIMILARITY_THRESHOLD_ACCEPT_ALL;

    /**
     * Exact-match metadata filters.
     */
    @Builder.Default
    private Map<String, Object> metadataFilters = new LinkedHashMap<String, Object>();
}

