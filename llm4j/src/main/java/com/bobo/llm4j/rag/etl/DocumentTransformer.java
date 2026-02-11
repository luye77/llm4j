package com.bobo.llm4j.rag.etl;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.List;

/**
 * Transformer for processing a document batch.
 */
public interface DocumentTransformer {

    List<RagDocument> transform(List<RagDocument> documents);
}

