package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.rag.etl.DocumentReader;
import com.bobo.llm4j.rag.ingest.RagIngestionService;
import com.bobo.llm4j.rag.vectorstore.VectorStore;
import com.bobo.llm4j.rag.vectorstore.SearchRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RagIngestionServiceTest {

    @Test
    public void testIngestAllAndIncremental() throws Exception {
        MutableReader reader = new MutableReader();
        reader.docs = Arrays.asList(
                doc("s1", 0, "Java"),
                doc("s1", 1, "Spring")
        );

        CaptureVectorStore store = new CaptureVectorStore();
        RagIngestionService service = new RagIngestionService(reader, null, new FixedEmbeddingModel(), store);

        int full = service.ingestAll();
        Assert.assertEquals(2, full);
        Assert.assertEquals(2, store.totalAdded);

        int incrementalNoChange = service.ingestIncremental();
        Assert.assertEquals(0, incrementalNoChange);
        Assert.assertEquals(2, store.totalAdded);

        reader.docs = Arrays.asList(
                doc("s1", 0, "Java"),
                doc("s1", 1, "Spring Boot")
        );
        int incrementalChanged = service.ingestIncremental();
        Assert.assertEquals(1, incrementalChanged);
        Assert.assertEquals(3, store.totalAdded);
    }

    private RagDocument doc(String source, int chunkIndex, String text) {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("source", source);
        metadata.put("chunk_index", chunkIndex);
        return RagDocument.builder().text(text).metadata(metadata).build();
    }

    private static class MutableReader implements DocumentReader {
        private List<RagDocument> docs = new ArrayList<RagDocument>();

        @Override
        public List<RagDocument> read() {
            return docs;
        }
    }

    private static class FixedEmbeddingModel implements EmbeddingModel {
        @Override
        public List<List<Double>> embedAll(List<String> texts) {
            List<List<Double>> vectors = new ArrayList<List<Double>>();
            for (String text : texts) {
                double v = text == null ? 0d : text.length();
                vectors.add(Arrays.asList(v, 1.0d));
            }
            return vectors;
        }
    }

    private static class CaptureVectorStore implements VectorStore {
        private int totalAdded;

        @Override
        public void add(List<RagDocument> documents, List<List<Double>> vectors) {
            totalAdded += documents.size();
        }

        @Override
        public List<RagDocument> similaritySearch(SearchRequest request, List<Double> queryEmbedding) {
            return new ArrayList<RagDocument>();
        }

        @Override
        public void deleteByMetadata(String key, Object value) {
        }
    }
}

