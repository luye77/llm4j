package com.bobo.llm4j.memory;

import com.bobo.llm4j.annotation.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 长期记忆存储接口 - 可扩展实现（内存、文件、数据库）
 *
 * @author bobo
 * @since 1.0.0
 */
public interface LongTermMemoryStore {
    
    /**
     * 根据ID查找记忆条目
     *
     * @param conversationId 会话ID
     * @param id 记忆条目ID
     * @return 记忆条目（如果不存在则返回空）
     */
    Optional<MemoryEntry> findById(String conversationId, String id);
    
    /**
     * 查找所有记忆条目
     *
     * @param conversationId 会话ID
     * @return 记忆条目列表
     */
    List<MemoryEntry> findAll(String conversationId);
    
    /**
     * 根据类型查找记忆条目
     *
     * @param conversationId 会话ID
     * @param type 记忆类型
     * @return 记忆条目列表
     */
    List<MemoryEntry> findByType(String conversationId, MemoryType type);
    
    /**
     * 根据标签查找记忆条目
     *
     * @param conversationId 会话ID
     * @param tag 标签
     * @return 记忆条目列表
     */
    List<MemoryEntry> findByTag(String conversationId, String tag);
    
    /**
     * 保存记忆条目（创建或更新）
     *
     * @param conversationId 会话ID
     * @param entry 记忆条目
     */
    void save(String conversationId, MemoryEntry entry);
    
    /**
     * 批量保存记忆条目
     *
     * @param conversationId 会话ID
     * @param entries 记忆条目列表
     */
    void saveAll(String conversationId, List<MemoryEntry> entries);
    
    /**
     * 删除记忆条目
     *
     * @param conversationId 会话ID
     * @param id 记忆条目ID
     */
    void delete(String conversationId, String id);
    
    /**
     * 删除指定会话的所有记忆条目
     *
     * @param conversationId 会话ID
     */
    void deleteAll(String conversationId);
    
    /**
     * 获取所有会话ID
     *
     * @return 会话ID列表
     */
    List<String> findAllConversationIds();
}
