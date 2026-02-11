package com.bobo.llm4j.memory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 记忆提炼服务 - 从 Daily Notes 提取重要信息到 Long-Term Memory
 * <p>
 * 提炼策略：
 * <ul>
 *   <li>基于规则的提取（简单快速）</li>
 *   <li>识别决策、偏好、上下文、经验教训</li>
 *   <li>支持定期触发或手动触发</li>
 * </ul>
 * <p>
 * 线程安全：使用调度线程池异步处理
 *
 * @author bobo
 * @since 1.0.0
 */
public final class MemoryDistillationService {
    
    private static final int DEFAULT_THRESHOLD = 10; // 默认阈值：10条对话
    private static final long DEFAULT_DEBOUNCE_MS = 5000; // 防抖：5秒
    
    private final DailyNotesStore dailyNotesStore;
    private final LongTermMemoryStore longTermMemoryStore;
    private final int threshold;
    private final ScheduledExecutorService scheduler;
    
    // conversationId -> (date -> scheduled task)
    private final Map<String, Map<LocalDate, ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();
    
    // conversationId -> date -> last entry count
    private final Map<String, Map<LocalDate, Integer>> lastProcessedCounts = new ConcurrentHashMap<>();
    
    private MemoryDistillationService(Builder builder) {
        this.dailyNotesStore = builder.dailyNotesStore;
        this.longTermMemoryStore = builder.longTermMemoryStore;
        this.threshold = builder.threshold;
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "memory-distillation");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * 调度提炼任务（带防抖）
     *
     * @param conversationId 会话ID
     * @param date 日期
     */
    public void scheduleDistillation(String conversationId, LocalDate date) {
        if (conversationId == null || date == null) {
            return;
        }
        
        // 检查是否达到阈值
        Optional<DailyNote> noteOpt = dailyNotesStore.findByDate(conversationId, date);
        if (!noteOpt.isPresent()) {
            return;
        }
        
        DailyNote note = noteOpt.get();
        int currentCount = note.size();
        
        // 获取上次处理的数量
        int lastCount = lastProcessedCounts
                .computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                .getOrDefault(date, 0);
        
        // 如果增量未达到阈值，跳过
        if (currentCount - lastCount < threshold) {
            return;
        }
        
        // 取消之前的任务（防抖）
        Map<LocalDate, ScheduledFuture<?>> conversationTasks = 
            scheduledTasks.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>());
        
        ScheduledFuture<?> existingTask = conversationTasks.get(date);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
        }
        
        // 调度新任务
        ScheduledFuture<?> newTask = scheduler.schedule(
            () -> distill(conversationId, date),
            DEFAULT_DEBOUNCE_MS,
            TimeUnit.MILLISECONDS
        );
        
        conversationTasks.put(date, newTask);
    }
    
    /**
     * 立即执行提炼（同步）
     *
     * @param conversationId 会话ID
     * @param date 日期
     */
    public void distill(String conversationId, LocalDate date) {
        if (conversationId == null || date == null) {
            return;
        }
        
        try {
            Optional<DailyNote> noteOpt = dailyNotesStore.findByDate(conversationId, date);
            if (!noteOpt.isPresent()) {
                return;
            }
            
            DailyNote note = noteOpt.get();
            
            // 提取记忆条目
            List<MemoryEntry> extractedEntries = extractMemories(note);
            
            if (!extractedEntries.isEmpty()) {
                // 保存到长期记忆
                longTermMemoryStore.saveAll(conversationId, extractedEntries);
                
                // 更新处理计数
                lastProcessedCounts
                    .computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                    .put(date, note.size());
            }
        } catch (Exception e) {
            // 记录异常但不抛出，避免影响主流程
            System.err.println("Memory distillation failed for " + conversationId + " on " + date + ": " + e.getMessage());
        }
    }
    
    /**
     * 提取记忆条目（基于规则）
     */
    private List<MemoryEntry> extractMemories(DailyNote note) {
        List<MemoryEntry> memories = new ArrayList<>();
        
        for (DailyEntry entry : note.getEntries()) {
            // 1. 提取决策
            memories.addAll(extractDecisions(entry, note.getDate()));
            
            // 2. 提取用户偏好
            memories.addAll(extractPreferences(entry, note.getDate()));
            
            // 3. 提取上下文
            memories.addAll(extractContext(entry, note.getDate()));
            
            // 4. 提取经验教训
            memories.addAll(extractLessons(entry, note.getDate()));
        }
        
        return memories;
    }
    
    /**
     * 提取决策
     */
    private List<MemoryEntry> extractDecisions(DailyEntry entry, LocalDate date) {
        List<MemoryEntry> decisions = new ArrayList<>();
        
        // 如果 entry 已经标记了 decision，直接使用
        if (entry.getDecision() != null && !entry.getDecision().trim().isEmpty()) {
            decisions.add(MemoryEntry.builder()
                    .type(MemoryType.KEY_DECISION)
                    .content(entry.getDecision())
                    .sourceDate(date.toString())
                    .addTag("decision")
                    .build());
            return decisions;
        }
        
        // 基于关键词提取决策
        Pattern decisionPattern = Pattern.compile(
            "(决定|选择|采用|使用).{1,100}",
            Pattern.CASE_INSENSITIVE
        );
        
        String text = combineMessages(entry);
        Matcher matcher = decisionPattern.matcher(text);
        
        while (matcher.find()) {
            String decision = matcher.group().trim();
            if (decision.length() > 10) { // 过滤太短的匹配
                decisions.add(MemoryEntry.builder()
                        .type(MemoryType.KEY_DECISION)
                        .content(decision)
                        .sourceDate(date.toString())
                        .addTag("decision")
                        .build());
            }
        }
        
        return decisions;
    }
    
    /**
     * 提取用户偏好
     */
    private List<MemoryEntry> extractPreferences(DailyEntry entry, LocalDate date) {
        List<MemoryEntry> preferences = new ArrayList<>();
        
        if (entry.getUserMessage() == null) {
            return preferences;
        }
        
        Pattern preferencePattern = Pattern.compile(
            "(我喜欢|我倾向|我偏好|我习惯|我通常).{1,100}",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = preferencePattern.matcher(entry.getUserMessage());
        
        while (matcher.find()) {
            String preference = matcher.group().trim();
            if (preference.length() > 10) {
                preferences.add(MemoryEntry.builder()
                        .type(MemoryType.USER_PREFERENCE)
                        .content(preference)
                        .sourceDate(date.toString())
                        .addTag("preference")
                        .build());
            }
        }
        
        return preferences;
    }
    
    /**
     * 提取上下文（高频关键词）
     */
    private List<MemoryEntry> extractContext(DailyEntry entry, LocalDate date) {
        List<MemoryEntry> contexts = new ArrayList<>();
        
        // 如果 entry 已经标记了 context，直接使用
        if (entry.getContext() != null && !entry.getContext().trim().isEmpty()) {
            contexts.add(MemoryEntry.builder()
                    .type(MemoryType.RECURRING_CONTEXT)
                    .content(entry.getContext())
                    .sourceDate(date.toString())
                    .addTag("context")
                    .build());
        }
        
        return contexts;
    }
    
    /**
     * 提取经验教训
     */
    private List<MemoryEntry> extractLessons(DailyEntry entry, LocalDate date) {
        List<MemoryEntry> lessons = new ArrayList<>();
        
        Pattern lessonPattern = Pattern.compile(
            "(问题|错误|注意|避免|教训|经验).{1,150}(解决|修复|方案|方法)",
            Pattern.CASE_INSENSITIVE
        );
        
        String text = combineMessages(entry);
        Matcher matcher = lessonPattern.matcher(text);
        
        while (matcher.find()) {
            String lesson = matcher.group().trim();
            if (lesson.length() > 15) {
                lessons.add(MemoryEntry.builder()
                        .type(MemoryType.LESSON_LEARNED)
                        .content(lesson)
                        .sourceDate(date.toString())
                        .addTag("lesson")
                        .build());
            }
        }
        
        return lessons;
    }
    
    /**
     * 合并用户和助手消息
     */
    private String combineMessages(DailyEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.getUserMessage() != null) {
            sb.append(entry.getUserMessage()).append(" ");
        }
        if (entry.getAssistantMessage() != null) {
            sb.append(entry.getAssistantMessage());
        }
        return sb.toString();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 创建构建器
     */
    public static Builder builder(DailyNotesStore dailyNotesStore, LongTermMemoryStore longTermMemoryStore) {
        return new Builder(dailyNotesStore, longTermMemoryStore);
    }
    
    /**
     * 构建器
     */
    public static final class Builder {
        private final DailyNotesStore dailyNotesStore;
        private final LongTermMemoryStore longTermMemoryStore;
        private int threshold = DEFAULT_THRESHOLD;
        
        private Builder(DailyNotesStore dailyNotesStore, LongTermMemoryStore longTermMemoryStore) {
            if (dailyNotesStore == null) {
                throw new IllegalArgumentException("dailyNotesStore cannot be null");
            }
            if (longTermMemoryStore == null) {
                throw new IllegalArgumentException("longTermMemoryStore cannot be null");
            }
            this.dailyNotesStore = dailyNotesStore;
            this.longTermMemoryStore = longTermMemoryStore;
        }
        
        /**
         * 设置触发阈值（默认：10条对话）
         */
        public Builder threshold(int threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("threshold must be greater than 0");
            }
            this.threshold = threshold;
            return this;
        }
        
        /**
         * 构建服务
         */
        public MemoryDistillationService build() {
            return new MemoryDistillationService(this);
        }
    }
}
