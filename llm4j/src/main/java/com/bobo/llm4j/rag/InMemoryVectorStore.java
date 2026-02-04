package com.bobo.llm4j.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InMemoryVectorStore - 内存向量存储实现
 */
public class InMemoryVectorStore implements VectorStore {
    private final Map<String, VectorRecord> storage = new HashMap<>();

    @Override
    public synchronized void upsert(List<VectorRecord> records) {
        if (records == null) {
            return;
        }
        for (VectorRecord record : records) {
            if (record != null && record.getId() != null) {
                storage.put(record.getId(), record);
            }
        }
    }

    @Override
    public synchronized List<ScoredDocument> query(List<Float> vector, int topK) {
        List<ScoredDocument> results = new ArrayList<>();
        if (vector == null || vector.isEmpty() || topK <= 0) {
            return results;
        }

        for (VectorRecord record : storage.values()) {
            if (record.getVector() == null || record.getVector().isEmpty()) {
                continue;
            }
            double score = cosineSimilarity(vector, record.getVector());
            results.add(new ScoredDocument(record.toDocument(), score));
        }

        results.sort(Comparator.comparingDouble(ScoredDocument::getScore).reversed());
        if (results.size() > topK) {
            return new ArrayList<>(results.subList(0, topK));
        }
        return results;
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        int size = Math.min(a.size(), b.size());
        if (size == 0) {
            return 0d;
        }
        double dot = 0d;
        double normA = 0d;
        double normB = 0d;
        for (int i = 0; i < size; i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA == 0d || normB == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
