package com.bobo.llm4j.rag.ingest;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.rag.etl.DocumentReader;
import com.bobo.llm4j.rag.etl.DocumentTransformer;
import com.bobo.llm4j.rag.vectorstore.VectorStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ETL ingestion service with incremental update support.
 */
public class RagIngestionService {

    private final DocumentReader documentReader;
    private final List<DocumentTransformer> transformers;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final Map<String, String> fingerprintByChunk = new LinkedHashMap<String, String>();

    public RagIngestionService(DocumentReader documentReader,
                               List<DocumentTransformer> transformers,
                               EmbeddingModel embeddingModel,
                               VectorStore vectorStore) {
        this.documentReader = documentReader;
        this.transformers = transformers == null ? new ArrayList<DocumentTransformer>() : transformers;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * Full load for first startup.
     */
    public synchronized int ingestAll() throws Exception {
        List<RagDocument> documents = pipeline();
        if (documents.isEmpty()) {
            return 0;
        }
        List<String> texts = toTexts(documents);
        List<List<Double>> vectors = embeddingModel.embedAll(texts);
        vectorStore.add(documents, vectors);
        updateFingerprints(documents);
        return documents.size();
    }

    /**
     * Incremental update for changed markdown chunks.
     */
    public synchronized int ingestIncremental() throws Exception {
        List<RagDocument> documents = pipeline();
        List<RagDocument> changed = new ArrayList<RagDocument>();
        for (RagDocument document : documents) {
            String key = chunkKey(document);
            String fp = fingerprint(document);
            String old = fingerprintByChunk.get(key);
            if (!fp.equals(old)) {
                changed.add(document);
                fingerprintByChunk.put(key, fp);
            }
        }
        if (changed.isEmpty()) {
            return 0;
        }
        List<String> texts = toTexts(changed);
        List<List<Double>> vectors = embeddingModel.embedAll(texts);
        vectorStore.add(changed, vectors);
        return changed.size();
    }

    private List<RagDocument> pipeline() {
        List<RagDocument> docs = documentReader.read();
        for (DocumentTransformer transformer : transformers) {
            docs = transformer.transform(docs);
        }
        return docs == null ? new ArrayList<RagDocument>() : docs;
    }

    private List<String> toTexts(List<RagDocument> documents) {
        List<String> texts = new ArrayList<String>(documents.size());
        for (RagDocument document : documents) {
            texts.add(document.getText() == null ? "" : document.getText());
        }
        return texts;
    }

    private void updateFingerprints(List<RagDocument> docs) {
        for (RagDocument doc : docs) {
            fingerprintByChunk.put(chunkKey(doc), fingerprint(doc));
        }
    }

    private String chunkKey(RagDocument doc) {
        Object source = doc.getMetadata() == null ? null : doc.getMetadata().get("source");
        Object idx = doc.getMetadata() == null ? null : doc.getMetadata().get("chunk_index");
        return String.valueOf(source) + "::" + String.valueOf(idx);
    }

    private String fingerprint(RagDocument doc) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = (doc.getText() == null ? "" : doc.getText()) + "|" + String.valueOf(doc.getMetadata());
            byte[] raw = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("fingerprint failed", e);
        }
    }
}

