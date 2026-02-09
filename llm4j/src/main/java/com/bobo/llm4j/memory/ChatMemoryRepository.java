package com.bobo.llm4j.memory;

import com.bobo.llm4j.chat.entity.Message;

import java.util.List;

/**
 * ChatMemoryRepository - 会话记忆存储仓库接口
 * <p>
 * 参考 Spring AI 的 ChatMemoryRepository 设计
 * 负责消息的底层存储和检索
 * 
 * @author bobo
 * @since 1.0.0
 */
public interface ChatMemoryRepository {

    /**
     * 查找所有会话ID
     *
     * @return 会话ID列表
     */
    List<String> findConversationIds();

    /**
     * 根据会话ID查找消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<Message> findByConversationId(String conversationId);

    /**
     * 保存会话消息（替换式保存，非追加）
     * <p>
     * 该方法会替换指定会话ID的所有现有消息
     *
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    void saveAll(String conversationId, List<Message> messages);

    /**
     * 删除指定会话的所有消息
     *
     * @param conversationId 会话ID
     */
    void deleteByConversationId(String conversationId);
}
