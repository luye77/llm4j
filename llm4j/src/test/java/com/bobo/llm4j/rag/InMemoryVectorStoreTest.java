package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.vectorstore.InMemoryVectorStore;
import com.bobo.llm4j.rag.vectorstore.SearchRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryVectorStoreTest {

    @Test
    public void testSimilaritySearchAndMetadataFilter() {
        InMemoryVectorStore store = new InMemoryVectorStore();

        Map<String, Object> javaMeta = new LinkedHashMap<String, Object>();
        javaMeta.put("topic", "java");
        Map<String, Object> pyMeta = new LinkedHashMap<String, Object>();
        pyMeta.put("topic", "python");

        RagDocument d1 = RagDocument.builder().id("d1").text("Java intro").metadata(javaMeta).build();
        RagDocument d2 = RagDocument.builder().id("d2").text("Java stream").metadata(javaMeta).build();
        RagDocument d3 = RagDocument.builder().id("d3").text("Python intro").metadata(pyMeta).build();

        store.add(
                Arrays.asList(d1, d2, d3),
                Arrays.asList(
                        Arrays.asList(1.0d, 0.0d),
                        Arrays.asList(0.8d, 0.2d),
                        Arrays.asList(0.0d, 1.0d)
                )
        );

        Map<String, Object> filters = new LinkedHashMap<String, Object>();
        filters.put("topic", "java");

        SearchRequest request = SearchRequest.builder()
                .query("what is java")
                .topK(2)
                .similarityThreshold(0.5d)
                .metadataFilters(filters)
                .build();

        List<RagDocument> hits = store.similaritySearch(request, Arrays.asList(1.0d, 0.0d));
        Assert.assertEquals(2, hits.size());
        Assert.assertEquals("d1", hits.get(0).getId());
        Assert.assertEquals("d2", hits.get(1).getId());
        Assert.assertTrue(hits.get(0).getScore() >= hits.get(1).getScore());
    }

    @Test
    public void testDeleteByMetadata() {
        InMemoryVectorStore store = new InMemoryVectorStore();

        Map<String, Object> keep = new LinkedHashMap<String, Object>();
        keep.put("source", "keep");
        Map<String, Object> delete = new LinkedHashMap<String, Object>();
        delete.put("source", "delete");

        RagDocument d1 = RagDocument.builder().id("d1").text("A").metadata(keep).build();
        RagDocument d2 = RagDocument.builder().id("d2").text("B").metadata(delete).build();
        store.add(Arrays.asList(d1, d2), Arrays.asList(
                Arrays.asList(1.0d, 0.0d),
                Arrays.asList(0.0d, 1.0d)
        ));

        store.deleteByMetadata("source", "delete");

        SearchRequest request = SearchRequest.builder()
                .query("q")
                .topK(10)
                .similarityThreshold(0.0d)
                .build();
        List<RagDocument> hits = store.similaritySearch(request, Arrays.asList(1.0d, 0.0d));
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals("d1", hits.get(0).getId());
    }
}

