package com.bobo.llm4j.rag;

import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.service.ChatModel;
import com.bobo.llm4j.service.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * RagService - 最小可用的 RAG 流程
 */
public class RagService {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final Retriever retriever;
    private final ChatModel chatModel;

    public RagService(EmbeddingModel embeddingModel, String embeddingModelName, VectorStore vectorStore, ChatModel chatModel) {
        this.embeddingClient = new EmbeddingClient(embeddingModel, embeddingModelName);
        this.vectorStore = vectorStore;
        this.retriever = new SimpleRetriever(this.embeddingClient, vectorStore);
        this.chatModel = chatModel;
    }

    public void index(List<Document> documents) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<String> texts = new ArrayList<>();
        for (Document document : documents) {
            texts.add(document.getContent());
        }
        List<List<Float>> vectors = embeddingClient.embedAll(texts);
        List<VectorRecord> records = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            List<Float> vector = i < vectors.size() ? vectors.get(i) : null;
            records.add(new VectorRecord(document.getId(), document.getContent(), vector, document.getMetadata()));
        }
        vectorStore.upsert(records);
    }

    public List<ScoredDocument> retrieve(String query, int topK) throws Exception {
        return retriever.retrieve(query, topK);
    }

    public ChatResponse chat(String chatModelName, String question, int topK) throws Exception {
        List<ScoredDocument> contextDocs = retrieve(question, topK);
        Prompt prompt = buildPrompt(chatModelName, question, contextDocs);
        return chatModel.call(prompt);
    }

    public Prompt buildPrompt(String chatModelName, String question, List<ScoredDocument> contextDocs) {
        String context = buildContext(contextDocs);
        String systemContent = "你是一个RAG助手。请优先依据提供的上下文回答，如果上下文不足请说明。";
        if (!context.isEmpty()) {
            systemContent = systemContent + "\n\n上下文:\n" + context;
        }

        return Prompt.builder()
                .model(chatModelName)
                .message(Message.withSystem(systemContent))
                .message(Message.withUser(question))
                .build();
    }

    private String buildContext(List<ScoredDocument> contextDocs) {
        if (contextDocs == null || contextDocs.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n---\n");
        for (ScoredDocument doc : contextDocs) {
            if (doc != null && doc.getDocument() != null) {
                joiner.add(doc.getDocument().getContent());
            }
        }
        return joiner.toString();
    }
}
