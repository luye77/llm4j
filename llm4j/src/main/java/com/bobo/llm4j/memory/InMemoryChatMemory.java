package com.bobo.llm4j.memory;

import com.bobo.llm4j.platform.openai.chat.entity.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryChatMemory - 内存会话记忆实现 (参照 Spring AI 的 InMemoryChatMemory)
 */
public class InMemoryChatMemory implements ChatMemory {

    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (isBlank(conversationId) || messages == null || messages.isEmpty()) {
            return;
        }
        store.compute(conversationId, (id, existing) -> {
            List<Message> updated = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            updated.addAll(messages);
            return updated;
        });
    }

    @Override
    public List<Message> get(String conversationId) {
        if (isBlank(conversationId)) {
            return Collections.emptyList();
        }
        List<Message> messages = store.get(conversationId);
        if (messages == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(messages);
    }

    @Override
    public void clear(String conversationId) {
        if (isBlank(conversationId)) {
            return;
        }
        store.remove(conversationId);
    }

    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
