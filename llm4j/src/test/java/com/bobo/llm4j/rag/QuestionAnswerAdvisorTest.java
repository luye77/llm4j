package com.bobo.llm4j.rag;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.rag.advisor.QuestionAnswerAdvisor;
import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReader;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReaderConfig;
import com.bobo.llm4j.rag.retrieval.DocumentRetriever;
import com.bobo.llm4j.rag.retrieval.VectorStoreDocumentRetriever;
import com.bobo.llm4j.rag.transformer.TokenTextSplitter;
import com.bobo.llm4j.rag.vectorstore.InMemoryVectorStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionAnswerAdvisorTest {

    @Test
    public void testBeforeShouldInjectRetrievedContextAndPassFilter() throws IOException {
        final CaptureRetriever retriever = new CaptureRetriever();

        InMemoryVectorStore vectorStore = buildInMemoryVectorStore();
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(3)
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(retriever).build();

        Map<String, Object> context = new LinkedHashMap<String, Object>();
        Map<String, Object> filter = new LinkedHashMap<String, Object>();
        filter.put("tenant", "t1");
        context.put(QuestionAnswerAdvisor.FILTER_EXPRESSION, filter);

        ChatClientRequest request = new ChatClientRequest(
                Arrays.asList(Message.withUser("什么是JVM？")),
                null,
                context,
                null
        );

        ChatClientRequest advised = advisor.before(request, null);
        Assert.assertNotNull(advised);
        Assert.assertEquals("什么是JVM？", retriever.capturedQuery);
        Assert.assertEquals("t1", retriever.capturedFilters.get("tenant"));

        String advisedText = advised.getMessages().get(advised.getMessages().size() - 1).getContent().getText();
        Assert.assertTrue(advisedText.contains("Context information is below"));
        Assert.assertTrue(advisedText.contains("JVM 是 Java 虚拟机"));
        Assert.assertTrue(advised.getContext().containsKey(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS));
    }

    @NotNull
    private static InMemoryVectorStore buildInMemoryVectorStore() throws IOException {
        InMemoryVectorStoreTest.FixedEmbeddingModel embeddingModel = new InMemoryVectorStoreTest.FixedEmbeddingModel()
                .put("Java intro", 1.0d, 0.0d)
                .put("Java stream", 0.8d, 0.2d)
                .put("Python intro", 0.0d, 1.0d)
                .put("what is java", 1.0d, 0.0d);

        InMemoryVectorStore vectorStore = new InMemoryVectorStore(embeddingModel);

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
        Path tempFile = Files.createTempFile("rag-md-", ".md");
        MarkdownDocumentReader reader = new MarkdownDocumentReader(Arrays.asList(tempFile), config);

        List<RagDocument> documents = reader.read();
        TokenTextSplitter documentTransformer = new TokenTextSplitter();
        List<RagDocument> enrichedDocuments = documentTransformer.transform(documents);
        vectorStore.add(enrichedDocuments);
        return vectorStore;
    }

    private static class CaptureRetriever implements DocumentRetriever {
        private String capturedQuery;
        private Map<String, Object> capturedFilters = new LinkedHashMap<String, Object>();

        @Override
        public List<RagDocument> retrieve(String query, Map<String, Object> runtimeFilters) {
            this.capturedQuery = query;
            if (runtimeFilters != null) {
                this.capturedFilters.putAll(runtimeFilters);
            }
            List<RagDocument> docs = new ArrayList<RagDocument>();
            docs.add(RagDocument.of("JVM 是 Java 虚拟机。"));
            return docs;
        }
    }
}

