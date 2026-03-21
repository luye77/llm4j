package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.entity.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatMemory - 会话记忆接口
 * <p>
 * 参考 Spring AI 的 ChatMemory 设计
 * 负责管理会话的消息上下文，决定哪些消息应该保留
 * 
 * @author bobo
 * @since 1.0.0
 */
public interface ChatMemory {

    /**
     * 默认会话ID
     */
    String DEFAULT_CONVERSATION_ID = "default";

    /**
     * 会话ID上下文键名（用于 Advisor 参数传递）
     */
    String CONVERSATION_ID = "chat_memory_conversation_id";

    /**
     * 添加单条消息到指定会话
     *
     * @param conversationId 会话ID
     * @param message 消息
     */
    default void add(String conversationId, Message message) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        this.add(conversationId, messages);
    }

    /**
     * 添加消息到指定会话
     *
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    void add(String conversationId, List<Message> messages);

    /**
     * 获取会话消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<Message> get(String conversationId);

    /**
     * 清理会话记忆
     *
     * @param conversationId 会话ID
     */
    void clear(String conversationId);
}
