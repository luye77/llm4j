package com.bobo.llm4j.memory;

import com.bobo.llm4j.annotation.Nullable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 长期记忆条目 - 从日记中提炼的重要信息
 *
 * @author bobo
 * @since 1.0.0
 */
public final class MemoryEntry {
    
    private final String id;
    private final MemoryType type;
    private final String content;
    private final LocalDate createdDate;
    private final LocalDate lastUpdated;
    private final Set<String> tags;
    @Nullable
    private final String sourceDate;
    
    private MemoryEntry(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.content = builder.content;
        this.createdDate = builder.createdDate;
        this.lastUpdated = builder.lastUpdated;
        this.tags = Collections.unmodifiableSet(new HashSet<>(builder.tags));
        this.sourceDate = builder.sourceDate;
    }
    
    public String getId() {
        return id;
    }
    
    public MemoryType getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public LocalDate getCreatedDate() {
        return createdDate;
    }
    
    public LocalDate getLastUpdated() {
        return lastUpdated;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    
    @Nullable
    public String getSourceDate() {
        return sourceDate;
    }
    
    /**
     * 创建更新后的副本
     */
    public MemoryEntry withUpdatedContent(String newContent) {
        return builder()
                .id(this.id)
                .type(this.type)
                .content(newContent)
                .createdDate(this.createdDate)
                .lastUpdated(LocalDate.now())
                .tags(this.tags)
                .sourceDate(this.sourceDate)
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryEntry that = (MemoryEntry) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "MemoryEntry{" +
               "id='" + id + '\'' +
               ", type=" + type +
               ", content='" + (content != null && content.length() > 100 
                   ? content.substring(0, 100) + "..." 
                   : content) + '\'' +
               ", createdDate=" + createdDate +
               ", lastUpdated=" + lastUpdated +
               ", tags=" + tags +
               '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private MemoryType type = MemoryType.CUSTOM;
        private String content;
        private LocalDate createdDate = LocalDate.now();
        private LocalDate lastUpdated = LocalDate.now();
        private Set<String> tags = new HashSet<>();
        private String sourceDate;
        
        private Builder() {
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder type(MemoryType type) {
            this.type = type;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder createdDate(LocalDate createdDate) {
            this.createdDate = createdDate;
            return this;
        }
        
        public Builder lastUpdated(LocalDate lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        public Builder tags(Set<String> tags) {
            this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
            return this;
        }
        
        public Builder addTag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                this.tags.add(tag);
            }
            return this;
        }
        
        public Builder sourceDate(String sourceDate) {
            this.sourceDate = sourceDate;
            return this;
        }
        
        public MemoryEntry build() {
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("content cannot be null or empty");
            }
            if (type == null) {
                throw new IllegalArgumentException("type cannot be null");
            }
            return new MemoryEntry(this);
        }
    }
}
