package com.bobo.llm4j.memory;

import com.bobo.llm4j.platform.openai.chat.entity.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageWindowChatMemory - 基于窗口的会话记忆实现
 */
public class MessageWindowChatMemory extends InMemoryChatMemory {

    private final int maxMessages;

    public MessageWindowChatMemory(int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be greater than 0");
        }
        this.maxMessages = maxMessages;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (isBlank(conversationId) || messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> merged = new ArrayList<>(get(conversationId));
        merged.addAll(messages);
        if (merged.size() > maxMessages) {
            merged = new ArrayList<>(merged.subList(merged.size() - maxMessages, merged.size()));
        }
        super.clear(conversationId);
        super.add(conversationId, merged);
    }
}
