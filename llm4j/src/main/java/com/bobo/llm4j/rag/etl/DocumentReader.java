package com.bobo.llm4j.rag.etl;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.List;

/**
 * Source for loading a batch of documents.
 */
public interface DocumentReader {

    List<RagDocument> read();
}

