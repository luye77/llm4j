package com.bobo.llm4j.rag.transformer;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Generation;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.etl.DocumentTransformer;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional metadata enricher for section summaries.
 */
public class SummaryMetadataEnricher implements DocumentTransformer {

    private static final String DEFAULT_TEMPLATE = "请总结下面文档片段的关键主题，输出简明中文摘要（不超过120字）：\n\n%s";

    private final ChatModel chatModel;
    private final String modelName;
    private final boolean enabled;
    private final String summaryTemplate;

    @Builder
    public SummaryMetadataEnricher(ChatModel chatModel, String modelName, boolean enabled, String summaryTemplate) {
        this.chatModel = chatModel;
        this.modelName = modelName;
        this.enabled = enabled;
        this.summaryTemplate = summaryTemplate == null || summaryTemplate.trim().isEmpty()
                ? DEFAULT_TEMPLATE : summaryTemplate;
    }

    @Override
    public List<RagDocument> transform(List<RagDocument> documents) {
        if (!enabled || chatModel == null || documents == null || documents.isEmpty()) {
            return documents;
        }
        List<RagDocument> output = new ArrayList<RagDocument>(documents.size());
        for (RagDocument document : documents) {
            output.add(enrich(document));
        }
        return output;
    }

    private RagDocument enrich(RagDocument doc) {
        if (doc == null || doc.getText() == null || doc.getText().trim().isEmpty()) {
            return doc;
        }
        try {
            Prompt prompt = Prompt.builder()
                    .model(modelName == null ? "default" : modelName)
                    .message(Message.withUser(String.format(summaryTemplate, doc.getText())))
                    .build();
            ChatResponse response = chatModel.call(prompt);
            String summary = extractText(response);
            if (summary == null || summary.trim().isEmpty()) {
                return doc;
            }
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            if (doc.getMetadata() != null) {
                metadata.putAll(doc.getMetadata());
            }
            metadata.put("section_summary", summary);
            return doc.toBuilder().metadata(metadata).build();
        } catch (Exception e) {
            return doc;
        }
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getGenerations() == null || response.getGenerations().isEmpty()) {
            return null;
        }
        Generation generation = response.getGenerations().get(0);
        if (generation == null || generation.getMessage() == null || generation.getMessage().getContent() == null) {
            return null;
        }
        return generation.getMessage().getContent().getText();
    }
}

