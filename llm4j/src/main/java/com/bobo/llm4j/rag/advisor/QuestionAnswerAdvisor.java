package com.bobo.llm4j.rag.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.advisor.AdvisorChain;
import com.bobo.llm4j.chat.client.advisor.BaseAdvisor;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.enums.MessageType;
import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.retrieval.DocumentRetriever;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Naive RAG advisor: retrieve docs and augment user query.
 */
public class QuestionAnswerAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";
    public static final String FILTER_EXPRESSION = "qa_filter_map";

    private static final String DEFAULT_TEMPLATE =
            "{query}\n\n" +
            "Context information is below.\n" +
            "---------------------\n" +
            "{question_answer_context}\n" +
            "---------------------\n" +
            "Given the context information, answer the question. " +
            "If answer not found in context, say you don't know.";

    private final DocumentRetriever retriever;
    private final String promptTemplate;
    private final int order;

    private QuestionAnswerAdvisor(DocumentRetriever retriever, String promptTemplate, int order) {
        this.retriever = retriever;
        this.promptTemplate = promptTemplate == null || promptTemplate.trim().isEmpty() ? DEFAULT_TEMPLATE : promptTemplate;
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        if (request == null) {
            return null;
        }
        String query = extractUserText(request.getMessages());
        Map<String, Object> context = request.getContext() == null
                ? new LinkedHashMap<String, Object>()
                : request.getContext();
        Map<String, Object> filter = extractFilterMap(context.get(FILTER_EXPRESSION));

        List<RagDocument> documents;
        try {
            documents = retriever.retrieve(query, filter);
        } catch (Exception e) {
            documents = new ArrayList<RagDocument>();
        }

        String contextText = buildContext(documents);
        String advisedText = promptTemplate
                .replace("{query}", query == null ? "" : query)
                .replace("{question_answer_context}", contextText);

        List<Message> advisedMessages = replaceUserMessage(request.getMessages(), advisedText);
        context.put(RETRIEVED_DOCUMENTS, documents);

        return new ChatClientRequest(advisedMessages, request.getOptions(), context, request.getMedia());
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "QuestionAnswerAdvisor";
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    private String extractUserText(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null && MessageType.USER.getRole().equals(message.getRole())
                    && message.getContent() != null && message.getContent().getText() != null) {
                return message.getContent().getText();
            }
        }
        return "";
    }

    private String buildContext(List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (RagDocument doc : documents) {
            if (doc == null || doc.getText() == null) {
                continue;
            }
            sb.append(doc.getText()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private List<Message> replaceUserMessage(List<Message> messages, String advisedText) {
        List<Message> result = new ArrayList<Message>();
        if (messages == null || messages.isEmpty()) {
            result.add(Message.withUser(advisedText));
            return result;
        }
        boolean replaced = false;
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (!replaced
                    && i == messages.size() - 1
                    && message != null
                    && MessageType.USER.getRole().equals(message.getRole())) {
                result.add(Message.withUser(advisedText));
                replaced = true;
            } else {
                result.add(message);
            }
        }
        if (!replaced) {
            result.add(Message.withUser(advisedText));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFilterMap(Object filterObject) {
        if (filterObject instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) filterObject);
        }
        return new LinkedHashMap<String, Object>();
    }

    public static Builder builder(DocumentRetriever retriever) {
        return new Builder(retriever);
    }

    public static final class Builder {
        private final DocumentRetriever retriever;
        private String promptTemplate;
        private int order;

        private Builder(DocumentRetriever retriever) {
            this.retriever = retriever;
            this.order = 0;
        }

        public Builder promptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public QuestionAnswerAdvisor build() {
            return new QuestionAnswerAdvisor(this.retriever, this.promptTemplate, this.order);
        }
    }
}

