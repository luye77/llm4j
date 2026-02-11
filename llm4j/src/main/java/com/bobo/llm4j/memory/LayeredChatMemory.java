package com.bobo.llm4j.memory;

import com.bobo.llm4j.annotation.Nullable;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.enums.MessageType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 双层记忆管理器
 * <p>
 * 架构：
 * <ul>
 *   <li>Daily Notes：按日期存储对话流水账（append-only）</li>
 *   <li>Long-Term Memory：从 Daily Notes 提炼的重要信息</li>
 * </ul>
 * <p>
 * 工作流程：
 * <ul>
 *   <li>添加消息时：自动记录到当天的 Daily Notes</li>
 *   <li>获取消息时：构建记忆上下文（Recent Context + Long-Term Memory）</li>
 *   <li>定期提炼：将 Daily Notes 中的重要信息提取到 Long-Term Memory</li>
 * </ul>
 *
 * @author bobo
 * @since 1.0.0
 */
public final class LayeredChatMemory implements ChatMemory {
    
    private static final int DEFAULT_RECENT_DAYS = 2; // 默认读取最近2天

    /**
     * -- GETTER --
     *  获取 Daily Notes Store
     */
    @Getter
    private final DailyNotesStore dailyNotesStore;
    /**
     * -- GETTER --
     *  获取 Long-Term Memory Store
     */
    @Getter
    private final LongTermMemoryStore longTermMemoryStore;
    @Nullable
    private final MemoryDistillationService distillationService;
    private final int recentDays;
    
    private LayeredChatMemory(Builder builder) {
        this.dailyNotesStore = builder.dailyNotesStore;
        this.longTermMemoryStore = builder.longTermMemoryStore;
        this.distillationService = builder.distillationService;
        this.recentDays = builder.recentDays;
    }
    
    @Override
    public void add(String conversationId, List<Message> messages) {
        validateConversationId(conversationId);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        // 1. 提取用户消息和助手消息
        String userMessage = extractLastUserMessage(messages);
        String assistantMessage = extractLastAssistantMessage(messages);
        
        if (userMessage == null && assistantMessage == null) {
            return; // 没有需要记录的消息
        }
        
        // 2. 创建 DailyEntry
        DailyEntry entry = DailyEntry.builder()
                .timestamp(LocalDateTime.now())
                .userMessage(userMessage)
                .assistantMessage(assistantMessage)
                .build();
        
        // 3. 保存到今天的 Daily Note
        LocalDate today = LocalDate.now();
        DailyNote todayNote = dailyNotesStore.findByDate(conversationId, today)
                .orElseGet(() -> new DailyNote(today));
        
        todayNote.append(entry);
        dailyNotesStore.save(conversationId, todayNote);
        
        // 4. 触发提炼服务（如果已配置）
        if (distillationService != null) {
            distillationService.scheduleDistillation(conversationId, today);
        }
    }
    
    @Override
    public List<Message> get(String conversationId) {
        validateConversationId(conversationId);
        
        // 构建记忆上下文，返回为 Message 列表
        String memoryContext = buildMemoryContext(conversationId);
        
        if (memoryContext.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 将记忆上下文封装为一个 SystemMessage
        return Collections.singletonList(Message.withSystem(memoryContext));
    }
    
    @Override
    public void clear(String conversationId) {
        validateConversationId(conversationId);
        dailyNotesStore.deleteAll(conversationId);
        longTermMemoryStore.deleteAll(conversationId);
    }
    
    /**
     * 构建记忆上下文（核心方法）
     * <p>
     * 合并 Recent Context (Daily Notes) + Long-Term Memory
     *
     * @param conversationId 会话ID
     * @return 记忆上下文字符串
     */
    public String buildMemoryContext(String conversationId) {
        validateConversationId(conversationId);
        
        StringBuilder context = new StringBuilder();
        
        // 1. 添加长期记忆（优先级高，放在前面）
        List<MemoryEntry> longTermEntries = longTermMemoryStore.findAll(conversationId);
        if (!longTermEntries.isEmpty()) {
            context.append("=== Long-Term Memory ===\n\n");
            context.append(formatLongTermMemory(longTermEntries));
            context.append("\n\n");
        }
        
        // 2. 添加近期上下文（最近N天的 Daily Notes）
        List<DailyNote> recentNotes = dailyNotesStore.findRecent(conversationId, recentDays);
        if (!recentNotes.isEmpty()) {
            context.append("=== Recent Context (Last ").append(recentDays).append(" Days) ===\n\n");
            context.append(formatDailyNotes(recentNotes));
        }
        
        return context.toString();
    }
    
    /**
     * 格式化长期记忆为可读文本
     */
    private String formatLongTermMemory(List<MemoryEntry> entries) {
        if (entries.isEmpty()) {
            return "";
        }
        
        // 按类型分组
        return entries.stream()
                .collect(Collectors.groupingBy(MemoryEntry::getType))
                .entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e1.getKey().ordinal(), e2.getKey().ordinal()))
                .map(entry -> {
                    StringBuilder section = new StringBuilder();
                    section.append("## ").append(entry.getKey().getDescription()).append("\n");
                    
                    for (MemoryEntry memoryEntry : entry.getValue()) {
                        section.append("- ").append(memoryEntry.getContent());
                        if (!memoryEntry.getTags().isEmpty()) {
                            section.append(" [").append(String.join(", ", memoryEntry.getTags())).append("]");
                        }
                        section.append("\n");
                    }
                    
                    return section.toString();
                })
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * 格式化 Daily Notes 为可读文本
     */
    private String formatDailyNotes(List<DailyNote> notes) {
        if (notes.isEmpty()) {
            return "";
        }
        
        return notes.stream()
                .sorted((n1, n2) -> n2.getDate().compareTo(n1.getDate())) // 最新的在前
                .map(note -> {
                    StringBuilder section = new StringBuilder();
                    section.append("## ").append(note.getDate()).append("\n");
                    
                    for (DailyEntry entry : note.getEntries()) {
                        section.append("- ").append(entry.getTimestamp().toLocalTime());
                        
                        if (entry.getUserMessage() != null) {
                            section.append(" User: ").append(truncate(entry.getUserMessage(), 200));
                        }
                        
                        if (entry.getAssistantMessage() != null) {
                            section.append(" | Assistant: ").append(truncate(entry.getAssistantMessage(), 200));
                        }
                        
                        if (entry.getDecision() != null) {
                            section.append(" | Decision: ").append(entry.getDecision());
                        }
                        
                        section.append("\n");
                    }
                    
                    return section.toString();
                })
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * 提取最后一条用户消息
     */
    @Nullable
    private String extractLastUserMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null && MessageType.USER.getRole().equals(message.getRole())) {
                return message.getContent() != null ? message.getContent().getText() : null;
            }
        }
        return null;
    }
    
    /**
     * 提取最后一条助手消息
     */
    @Nullable
    private String extractLastAssistantMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null && MessageType.ASSISTANT.getRole().equals(message.getRole())) {
                return message.getContent() != null ? message.getContent().getText() : null;
            }
        }
        return null;
    }
    
    /**
     * 截断文本到指定长度
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    private void validateConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 构建器
     */
    public static final class Builder {
        private DailyNotesStore dailyNotesStore = new InMemoryDailyNotesStore();
        private LongTermMemoryStore longTermMemoryStore = new InMemoryLongTermMemoryStore();
        private MemoryDistillationService distillationService;
        private int recentDays = DEFAULT_RECENT_DAYS;
        
        private Builder() {
        }
        
        /**
         * 设置 Daily Notes Store（默认：内存存储）
         */
        public Builder dailyNotesStore(DailyNotesStore dailyNotesStore) {
            if (dailyNotesStore == null) {
                throw new IllegalArgumentException("dailyNotesStore cannot be null");
            }
            this.dailyNotesStore = dailyNotesStore;
            return this;
        }
        
        /**
         * 设置 Long-Term Memory Store（默认：内存存储）
         */
        public Builder longTermMemoryStore(LongTermMemoryStore longTermMemoryStore) {
            if (longTermMemoryStore == null) {
                throw new IllegalArgumentException("longTermMemoryStore cannot be null");
            }
            this.longTermMemoryStore = longTermMemoryStore;
            return this;
        }
        
        /**
         * 设置记忆提炼服务（可选）
         */
        public Builder distillationService(MemoryDistillationService distillationService) {
            this.distillationService = distillationService;
            return this;
        }
        
        /**
         * 设置读取最近N天的 Daily Notes（默认：2天）
         */
        public Builder recentDays(int recentDays) {
            if (recentDays <= 0) {
                throw new IllegalArgumentException("recentDays must be greater than 0");
            }
            this.recentDays = recentDays;
            return this;
        }
        
        /**
         * 构建 LayeredChatMemory
         */
        public LayeredChatMemory build() {
            return new LayeredChatMemory(this);
        }
    }
}
