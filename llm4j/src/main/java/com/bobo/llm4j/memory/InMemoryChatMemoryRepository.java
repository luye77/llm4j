package com.bobo.llm4j.memory;

import com.bobo.llm4j.platform.openai.chat.entity.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryChatMemoryRepository - 内存存储实现
 * <p>
 * 参考 Spring AI 的 InMemoryChatMemoryRepository 设计
 * 使用 ConcurrentHashMap 实现线程安全的内存存储
 *
 * @author bobo
 * @since 1.0.0
 */
public final class InMemoryChatMemoryRepository implements ChatMemoryRepository {

    private final Map<String, List<Message>> chatMemoryStore = new ConcurrentHashMap<>();

    @Override
    public List<String> findConversationIds() {
        return new ArrayList<>(chatMemoryStore.keySet());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        
        List<Message> messages = chatMemoryStore.get(conversationId);
        return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        if (messages == null) {
            throw new IllegalArgumentException("messages cannot be null");
        }
        
        // 检查消息列表中是否有 null 元素
        for (Message message : messages) {
            if (message == null) {
                throw new IllegalArgumentException("messages cannot contain null elements");
            }
        }
        
        chatMemoryStore.put(conversationId, new ArrayList<>(messages));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        chatMemoryStore.remove(conversationId);
    }
}
