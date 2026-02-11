package com.bobo.llm4j.memory;

import com.bobo.llm4j.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 日记存储接口 - 可扩展实现（内存、文件、数据库）
 *
 * @author bobo
 * @since 1.0.0
 */
public interface DailyNotesStore {
    
    /**
     * 根据日期查找日记
     *
     * @param conversationId 会话ID
     * @param date 日期
     * @return 日记（如果不存在则返回空）
     */
    Optional<DailyNote> findByDate(String conversationId, LocalDate date);
    
    /**
     * 查找最近N天的日记
     *
     * @param conversationId 会话ID
     * @param days 天数
     * @return 日记列表（按日期倒序，最新的在前）
     */
    List<DailyNote> findRecent(String conversationId, int days);
    
    /**
     * 查找日期范围内的日记
     *
     * @param conversationId 会话ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 日记列表（按日期升序）
     */
    List<DailyNote> findByDateRange(String conversationId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 保存日记（创建或更新）
     *
     * @param conversationId 会话ID
     * @param dailyNote 日记
     */
    void save(String conversationId, DailyNote dailyNote);
    
    /**
     * 删除指定日期的日记
     *
     * @param conversationId 会话ID
     * @param date 日期
     */
    void delete(String conversationId, LocalDate date);
    
    /**
     * 删除指定会话的所有日记
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
