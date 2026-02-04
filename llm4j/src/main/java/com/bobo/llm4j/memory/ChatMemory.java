package com.bobo.llm4j.memory;

import com.bobo.llm4j.platform.openai.chat.entity.Message;

import java.util.List;

/**
 * ChatMemory - 会话记忆接口 (参照 Spring AI 的 ChatMemory)
 */
public interface ChatMemory {

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
