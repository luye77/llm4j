package com.bobo.llm4j.rag.config;

import com.bobo.llm4j.rag.embedding.EmbeddingProvider;
import lombok.Builder;
import lombok.Data;

/**
 * RAG runtime options.
 */
@Data
@Builder
public class RagOptions {

    @Builder.Default
    private EmbeddingProvider embeddingProvider = EmbeddingProvider.QWEN;

    @Builder.Default
    private String qwenEmbeddingModel = "text-embedding-v3";

    @Builder.Default
    private String openAiEmbeddingModel = "text-embedding-3-small";

    @Builder.Default
    private int topK = 4;

    @Builder.Default
    private double similarityThreshold = 0.5d;

    @Builder.Default
    private boolean summaryMetadataEnabled = false;

    @Builder.Default
    private boolean includeSourceCitationByDefault = false;
}

