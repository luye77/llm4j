package com.bobo.llm4j.rag.retrieval;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.rag.vectorstore.SearchRequest;
import com.bobo.llm4j.rag.vectorstore.VectorStore;
import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vector store based document retriever.
 */
public class VectorStoreDocumentRetriever implements DocumentRetriever {

    public static final String FILTER_EXPRESSION = "rag_filter_map";

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final Double similarityThreshold;
    private final Integer topK;
    private final Map<String, Object> defaultFilters;

    @Builder
    public VectorStoreDocumentRetriever(VectorStore vectorStore,
                                        EmbeddingModel embeddingModel,
                                        Double similarityThreshold,
                                        Integer topK,
                                        Map<String, Object> defaultFilters) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold == null ? SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL : similarityThreshold;
        this.topK = topK == null ? SearchRequest.DEFAULT_TOP_K : topK;
        this.defaultFilters = defaultFilters == null ? new LinkedHashMap<String, Object>() : defaultFilters;
    }

    @Override
    public List<RagDocument> retrieve(String query, Map<String, Object> runtimeFilters) throws Exception {
        List<Double> queryVector = embeddingModel.embed(query == null ? "" : query);
        Map<String, Object> filters = new LinkedHashMap<String, Object>(this.defaultFilters);
        if (runtimeFilters != null) {
            filters.putAll(runtimeFilters);
        }
        SearchRequest request = SearchRequest.builder()
                .query(query == null ? "" : query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .metadataFilters(filters)
                .build();
        return vectorStore.similaritySearch(request, queryVector);
    }
}

