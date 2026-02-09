package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.enums.MessageType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MessageWindowChatMemory - 基于滑动窗口的会话记忆实现
 * <p>
 * 参考 Spring AI 的 MessageWindowChatMemory 设计
 * 维护固定大小的消息窗口，超过限制时删除旧消息，但始终保留 SystemMessage
 * 
 * <p>特性：
 * <ul>
 *   <li>维护指定大小的消息窗口（默认20条）</li>
 *   <li>新增 SystemMessage 时，移除所有旧的 SystemMessage</li>
 *   <li>消息超限时，优先保留 SystemMessage，删除其他类型的旧消息</li>
 * </ul>
 *
 * @author bobo
 * @since 1.0.0
 */
public final class MessageWindowChatMemory implements ChatMemory {

    private static final int DEFAULT_MAX_MESSAGES = 20;

    private final ChatMemoryRepository chatMemoryRepository;
    private final int maxMessages;

    /**
     * 使用默认配置构造（默认使用内存存储，最大20条消息）
     */
    public MessageWindowChatMemory() {
        this(new InMemoryChatMemoryRepository(), DEFAULT_MAX_MESSAGES);
    }

    /**
     * 使用指定的最大消息数构造（使用内存存储）
     *
     * @param maxMessages 最大消息数
     */
    public MessageWindowChatMemory(int maxMessages) {
        this(new InMemoryChatMemoryRepository(), maxMessages);
    }

    /**
     * 使用指定的存储仓库和最大消息数构造
     *
     * @param chatMemoryRepository 存储仓库
     * @param maxMessages 最大消息数
     */
    public MessageWindowChatMemory(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        if (chatMemoryRepository == null) {
            throw new IllegalArgumentException("chatMemoryRepository cannot be null");
        }
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be greater than 0");
        }
        this.chatMemoryRepository = chatMemoryRepository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
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

        List<Message> memoryMessages = chatMemoryRepository.findByConversationId(conversationId);
        List<Message> processedMessages = process(memoryMessages, messages);
        chatMemoryRepository.saveAll(conversationId, processedMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        return chatMemoryRepository.findByConversationId(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        chatMemoryRepository.deleteByConversationId(conversationId);
    }

    /**
     * 处理新旧消息的合并和窗口限制
     * <p>
     * 核心逻辑参考 Spring AI：
     * 1. 如果新消息中有 SystemMessage，移除旧消息中的所有 SystemMessage
     * 2. 合并新旧消息
     * 3. 如果总数超过限制，保留 SystemMessage，删除其他旧消息
     *
     * @param memoryMessages 内存中的旧消息
     * @param newMessages 新消息
     * @return 处理后的消息列表
     */
    private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
        List<Message> processedMessages = new ArrayList<>();

        // 检查新消息中是否有 SystemMessage
        Set<Message> memoryMessagesSet = new HashSet<>(memoryMessages);
        boolean hasNewSystemMessage = newMessages.stream()
                .filter(this::isSystemMessage)
                .anyMatch(message -> !memoryMessagesSet.contains(message));

        // 如果有新的 SystemMessage，过滤掉旧的 SystemMessage
        memoryMessages.stream()
                .filter(message -> !(hasNewSystemMessage && isSystemMessage(message)))
                .forEach(processedMessages::add);

        // 添加新消息
        processedMessages.addAll(newMessages);

        // 如果没有超过限制，直接返回
        if (processedMessages.size() <= this.maxMessages) {
            return processedMessages;
        }

        // 应用窗口限制：保留 SystemMessage，删除其他旧消息
        int messagesToRemove = processedMessages.size() - this.maxMessages;
        List<Message> trimmedMessages = new ArrayList<>();
        int removed = 0;

        for (Message message : processedMessages) {
            // SystemMessage 始终保留，或已经删除足够多的消息
            if (isSystemMessage(message) || removed >= messagesToRemove) {
                trimmedMessages.add(message);
            } else {
                removed++;
            }
        }

        return trimmedMessages;
    }

    /**
     * 判断是否为 SystemMessage
     */
    private boolean isSystemMessage(Message message) {
        return message != null && MessageType.SYSTEM.getRole().equals(message.getRole());
    }

    /**
     * 获取最大消息数配置
     */
    public int getMaxMessages() {
        return maxMessages;
    }

    /**
     * 构建器模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        private int maxMessages = DEFAULT_MAX_MESSAGES;

        private Builder() {
        }

        public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
            this.chatMemoryRepository = chatMemoryRepository;
            return this;
        }

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public MessageWindowChatMemory build() {
            return new MessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
        }
    }
}
