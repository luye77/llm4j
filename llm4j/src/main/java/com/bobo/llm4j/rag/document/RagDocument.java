package com.bobo.llm4j.rag.document;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RAG document payload used in ETL and retrieval.
 */
@Data
@Builder(toBuilder = true)
public class RagDocument {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String text;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<String, Object>();

    /**
     * Optional similarity score generated at retrieval time.
     */
    private Double score;

    public static RagDocument of(String text) {
        return RagDocument.builder().text(text).build();
    }
}

