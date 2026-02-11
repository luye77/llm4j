package com.bobo.llm4j.memory;

import com.bobo.llm4j.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 日记条目 - 单次对话记录
 *
 * @author bobo
 * @since 1.0.0
 */
public final class DailyEntry {
    
    private final LocalDateTime timestamp;
    private final String userMessage;
    private final String assistantMessage;
    @Nullable
    private final String decision;
    @Nullable
    private final String context;
    
    private DailyEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.userMessage = builder.userMessage;
        this.assistantMessage = builder.assistantMessage;
        this.decision = builder.decision;
        this.context = builder.context;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public String getAssistantMessage() {
        return assistantMessage;
    }
    
    @Nullable
    public String getDecision() {
        return decision;
    }
    
    @Nullable
    public String getContext() {
        return context;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyEntry that = (DailyEntry) o;
        return Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(userMessage, that.userMessage) &&
               Objects.equals(assistantMessage, that.assistantMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, userMessage, assistantMessage);
    }
    
    @Override
    public String toString() {
        return "DailyEntry{" +
               "timestamp=" + timestamp +
               ", userMessage='" + (userMessage != null && userMessage.length() > 50 
                   ? userMessage.substring(0, 50) + "..." 
                   : userMessage) + '\'' +
               ", assistantMessage='" + (assistantMessage != null && assistantMessage.length() > 50 
                   ? assistantMessage.substring(0, 50) + "..." 
                   : assistantMessage) + '\'' +
               ", decision='" + decision + '\'' +
               '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private LocalDateTime timestamp = LocalDateTime.now();
        private String userMessage;
        private String assistantMessage;
        private String decision;
        private String context;
        
        private Builder() {
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }
        
        public Builder assistantMessage(String assistantMessage) {
            this.assistantMessage = assistantMessage;
            return this;
        }
        
        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }
        
        public Builder context(String context) {
            this.context = context;
            return this;
        }
        
        public DailyEntry build() {
            if (userMessage == null && assistantMessage == null) {
                throw new IllegalArgumentException("至少需要提供 userMessage 或 assistantMessage");
            }
            return new DailyEntry(this);
        }
    }
}
