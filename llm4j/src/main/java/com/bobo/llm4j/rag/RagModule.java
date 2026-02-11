package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.advisor.QuestionAnswerAdvisor;
import com.bobo.llm4j.rag.ingest.RagIngestionService;
import com.bobo.llm4j.rag.retrieval.VectorStoreDocumentRetriever;
import com.bobo.llm4j.rag.vectorstore.VectorStore;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Aggregated RAG components.
 */
@Data
@AllArgsConstructor
public class RagModule {

    private RagIngestionService ingestionService;
    private QuestionAnswerAdvisor questionAnswerAdvisor;
    private VectorStoreDocumentRetriever retriever;
    private VectorStore vectorStore;
}

