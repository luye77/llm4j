package com.bobo.llm4j.rag;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.rag.advisor.QuestionAnswerAdvisor;
import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.retrieval.DocumentRetriever;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionAnswerAdvisorTest {

    @Test
    public void testBeforeShouldInjectRetrievedContextAndPassFilter() {
        final CaptureRetriever retriever = new CaptureRetriever();
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

