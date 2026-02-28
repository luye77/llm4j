package com.bobo.llm4j.integration;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.retrieval.DocumentRetriever;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简单 DocumentRetriever：返回固定文档，用于测试 RAG 流程（无需 embedding）
 */
public class SimpleDocumentRetriever implements DocumentRetriever {

    private final List<RagDocument> documents;

    public SimpleDocumentRetriever(List<RagDocument> documents) {
        this.documents = documents != null ? new ArrayList<>(documents) : new ArrayList<>();
    }

    @Override
    public List<RagDocument> retrieve(String query, Map<String, Object> runtimeFilters) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // 简单实现：根据关键词返回相关文档
        List<RagDocument> result = new ArrayList<>();
        for (RagDocument doc : documents) {
            String text = doc.getText();
            if (text != null && (query.contains("llm4j") || query.contains("工具") || query.contains("RAG"))) {
                result.add(doc);
            }
        }
        return result.isEmpty() ? new ArrayList<>(documents) : result;
    }
}
