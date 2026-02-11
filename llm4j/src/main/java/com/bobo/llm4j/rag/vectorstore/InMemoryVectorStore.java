package com.bobo.llm4j.rag.vectorstore;

import com.bobo.llm4j.rag.document.RagDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory vector store for local RAG.
 */
public class InMemoryVectorStore implements VectorStore {

    private final List<Entry> entries = new CopyOnWriteArrayList<Entry>();

    @Override
    public void add(List<RagDocument> documents, List<List<Double>> vectors) {
        if (documents == null || vectors == null || documents.size() != vectors.size()) {
            throw new IllegalArgumentException("documents and vectors size must match");
        }
        for (int i = 0; i < documents.size(); i++) {
            RagDocument doc = documents.get(i);
            List<Double> vector = vectors.get(i);
            if (doc == null || vector == null || vector.isEmpty()) {
                continue;
            }
            this.entries.add(new Entry(doc, new ArrayList<Double>(vector)));
        }
    }

    @Override
    public List<RagDocument> similaritySearch(SearchRequest request, List<Double> queryEmbedding) {
        if (request == null || queryEmbedding == null || queryEmbedding.isEmpty()) {
            return Collections.emptyList();
        }
        List<RagDocument> hits = new ArrayList<RagDocument>();
        for (Entry entry : entries) {
            if (!matchMetadata(entry.document, request.getMetadataFilters())) {
                continue;
            }
            double score = cosineSimilarity(queryEmbedding, entry.vector);
            if (score < request.getSimilarityThreshold()) {
                continue;
            }
            RagDocument hit = entry.document.toBuilder().score(score).build();
            hits.add(hit);
        }
        Collections.sort(hits, new Comparator<RagDocument>() {
            @Override
            public int compare(RagDocument a, RagDocument b) {
                double sa = a.getScore() == null ? 0d : a.getScore();
                double sb = b.getScore() == null ? 0d : b.getScore();
                return Double.compare(sb, sa);
            }
        });
        int end = Math.min(Math.max(request.getTopK(), 0), hits.size());
        return new ArrayList<RagDocument>(hits.subList(0, end));
    }

    @Override
    public void deleteByMetadata(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            Object actual = entry.document.getMetadata() == null ? null : entry.document.getMetadata().get(key);
            if (value == null ? actual == null : value.equals(actual)) {
                entries.remove(entry);
            }
        }
    }

    private boolean matchMetadata(RagDocument doc, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        Map<String, Object> metadata = doc.getMetadata();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            Object actual = metadata == null ? null : metadata.get(entry.getKey());
            Object expect = entry.getValue();
            if (expect == null) {
                if (actual != null) {
                    return false;
                }
            } else if (!expect.equals(actual)) {
                return false;
            }
        }
        return true;
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        int len = Math.min(left.size(), right.size());
        if (len == 0) {
            return 0d;
        }
        double dot = 0d;
        double normLeft = 0d;
        double normRight = 0d;
        for (int i = 0; i < len; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            normLeft += l * l;
            normRight += r * r;
        }
        if (normLeft == 0d || normRight == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(normLeft) * Math.sqrt(normRight));
    }

    private static final class Entry {
        private final RagDocument document;
        private final List<Double> vector;

        private Entry(RagDocument document, List<Double> vector) {
            this.document = document;
            this.vector = vector;
        }
    }
}

