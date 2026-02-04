package com.bobo.llm4j.rag;

import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Generation;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.embedding.entity.Embedding;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import com.bobo.llm4j.service.ChatModel;
import com.bobo.llm4j.service.EmbeddingModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RagServiceTest {

    @Test
    public void testRetrieveReturnsMostRelevantDocument() throws Exception {
        EmbeddingModel embeddingModel = new FakeEmbeddingModel();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagService ragService = new RagService(embeddingModel, "fake-embed", vectorStore, new FakeChatModel());

        List<Document> docs = Arrays.asList(
                new Document("doc-1", "apple banana", null),
                new Document("doc-2", "car engine", null)
        );

        ragService.index(docs);

        List<ScoredDocument> results = ragService.retrieve("apple", 1);
        assertEquals(1, results.size());
        assertEquals("doc-1", results.get(0).getDocument().getId());
    }

    @Test
    public void testChatBuildsPromptWithContext() throws Exception {
        EmbeddingModel embeddingModel = new FakeEmbeddingModel();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        FakeChatModel chatModel = new FakeChatModel();
        RagService ragService = new RagService(embeddingModel, "fake-embed", vectorStore, chatModel);

        ragService.index(Collections.singletonList(
                new Document("doc-1", "apple banana", null)
        ));

        ragService.chat("fake-chat", "What is apple?", 1);

        Prompt prompt = chatModel.getLastPrompt();
        assertNotNull(prompt);
        assertEquals("fake-chat", prompt.getModel());
        assertEquals(2, prompt.getMessages().size());

        String systemText = prompt.getMessages().get(0).getContent().getText();
        assertTrue(systemText.contains("上下文"));
        assertTrue(systemText.contains("apple banana"));

        String userText = prompt.getMessages().get(1).getContent().getText();
        assertEquals("What is apple?", userText);
    }

    private static class FakeEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(String baseUrl, String apiKey, EmbeddingRequest request) {
            List<String> texts = toTexts(request.getInput());
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                embeddings.add(Embedding.builder()
                        .index(i)
                        .embedding(toVector(texts.get(i)))
                        .object("embedding")
                        .build());
            }
            return EmbeddingResponse.builder()
                    .model(request.getModel())
                    .data(embeddings)
                    .build();
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            return call(null, null, request);
        }

        private List<String> toTexts(Object input) {
            if (input instanceof String) {
                return Collections.singletonList((String) input);
            }
            if (input instanceof List) {
                List<String> texts = new ArrayList<>();
                for (Object item : (List<?>) input) {
                    texts.add(String.valueOf(item));
                }
                return texts;
            }
            return Collections.emptyList();
        }

        private List<Float> toVector(String text) {
            float aCount = 0f;
            float bCount = 0f;
            float length = text == null ? 0f : text.length();
            if (text != null) {
                for (char ch : text.toCharArray()) {
                    if (ch == 'a' || ch == 'A') {
                        aCount += 1f;
                    } else if (ch == 'b' || ch == 'B') {
                        bCount += 1f;
                    }
                }
            }
            return Arrays.asList(aCount, bCount, length);
        }
    }

    private static class FakeChatModel implements ChatModel {
        private Prompt lastPrompt;

        @Override
        public ChatResponse call(String baseUrl, String apiKey, Prompt prompt) {
            this.lastPrompt = prompt;
            Generation generation = new Generation();
            generation.setIndex(0);
            generation.setMessage(Message.withAssistant("ok"));

            ChatResponse response = new ChatResponse();
            response.setGenerations(Collections.singletonList(generation));
            return response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return call(null, null, prompt);
        }

        @Override
        public void stream(String baseUrl, String apiKey, Prompt prompt, com.bobo.llm4j.listener.StreamingResponseHandler handler) {
            throw new UnsupportedOperationException("stream not supported in test");
        }

        @Override
        public void stream(Prompt prompt, com.bobo.llm4j.listener.StreamingResponseHandler handler) {
            throw new UnsupportedOperationException("stream not supported in test");
        }

        public Prompt getLastPrompt() {
            return lastPrompt;
        }
    }
}
