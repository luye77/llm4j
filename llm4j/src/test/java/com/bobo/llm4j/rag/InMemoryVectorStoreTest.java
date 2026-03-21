package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.rag.vectorstore.InMemoryVectorStore;
import com.bobo.llm4j.rag.vectorstore.SearchRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class InMemoryVectorStoreTest {

    /**
     * Fixed embedding model that maps each text to a deterministic vector
     * so test assertions remain stable.
     */
    static class FixedEmbeddingModel implements EmbeddingModel {
        private final Map<String, List<Double>> textToVector = new LinkedHashMap<String, List<Double>>();

        FixedEmbeddingModel put(String text, Double... values) {
            textToVector.put(text, Arrays.asList(values));
            return this;
        }

        @Override
        public List<List<Double>> embedAll(List<String> texts) {
            List<List<Double>> result = new ArrayList<List<Double>>();
            for (String text : texts) {
                List<Double> vec = textToVector.get(text);
                result.add(vec != null ? vec : Arrays.asList(0.0d, 0.0d));
            }
            return result;
        }
    }

    @Test
    public void testSimilaritySearchAndMetadataFilter() {
        FixedEmbeddingModel embeddingModel = new FixedEmbeddingModel()
                .put("Java intro", 1.0d, 0.0d)
                .put("Java stream", 0.8d, 0.2d)
                .put("Python intro", 0.0d, 1.0d)
                .put("what is java", 1.0d, 0.0d);

        InMemoryVectorStore store = new InMemoryVectorStore(embeddingModel);

        Map<String, Object> javaMeta = new LinkedHashMap<String, Object>();
        javaMeta.put("topic", "java");
        Map<String, Object> pyMeta = new LinkedHashMap<String, Object>();
        pyMeta.put("topic", "python");

        RagDocument d1 = RagDocument.builder().id("d1").text("Java intro").metadata(javaMeta).build();
        RagDocument d2 = RagDocument.builder().id("d2").text("Java stream").metadata(javaMeta).build();
        RagDocument d3 = RagDocument.builder().id("d3").text("Python intro").metadata(pyMeta).build();

        store.add(Arrays.asList(d1, d2, d3));

        Map<String, Object> filters = new LinkedHashMap<String, Object>();
        filters.put("topic", "java");

        SearchRequest request = SearchRequest.builder()
                .query("what is java")
                .topK(2)
                .similarityThreshold(0.5d)
                .metadataFilters(filters)
                .build();

        List<RagDocument> hits = store.similaritySearch(request);
        Assert.assertEquals(2, hits.size());
        Assert.assertEquals("d1", hits.get(0).getId());
        Assert.assertEquals("d2", hits.get(1).getId());
        Assert.assertTrue(hits.get(0).getScore() >= hits.get(1).getScore());
    }

    @Test
    public void testDeleteByMetadata() {
        FixedEmbeddingModel embeddingModel = new FixedEmbeddingModel()
                .put("A", 1.0d, 0.0d)
                .put("B", 0.0d, 1.0d)
                .put("q", 1.0d, 0.0d);

        InMemoryVectorStore store = new InMemoryVectorStore(embeddingModel);

        Map<String, Object> keep = new LinkedHashMap<String, Object>();
        keep.put("source", "keep");
        Map<String, Object> delete = new LinkedHashMap<String, Object>();
        delete.put("source", "delete");

        RagDocument d1 = RagDocument.builder().id("d1").text("A").metadata(keep).build();
        RagDocument d2 = RagDocument.builder().id("d2").text("B").metadata(delete).build();
        store.add(Arrays.asList(d1, d2));

        store.deleteByMetadata("source", "delete");

        SearchRequest request = SearchRequest.builder()
                .query("q")
                .topK(10)
                .similarityThreshold(0.0d)
                .build();
        List<RagDocument> hits = store.similaritySearch(request);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals("d1", hits.get(0).getId());
    }

    @Test
    public void testWriteDelegatesToAdd() {
        FixedEmbeddingModel embeddingModel = new FixedEmbeddingModel()
                .put("Hello", 1.0d, 0.0d)
                .put("search", 1.0d, 0.0d);

        InMemoryVectorStore store = new InMemoryVectorStore(embeddingModel);
        RagDocument doc = RagDocument.builder().id("w1").text("Hello").build();

        store.write(Collections.singletonList(doc));

        SearchRequest request = SearchRequest.builder()
                .query("search")
                .topK(1)
                .similarityThreshold(0.0d)
                .build();
        List<RagDocument> hits = store.similaritySearch(request);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals("w1", hits.get(0).getId());
    }
}
