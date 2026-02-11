package com.bobo.llm4j.rag.etl;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.List;

/**
 * Sink for persisting a document batch.
 */
public interface DocumentWriter {

    void write(List<RagDocument> documents);
}

