package com.bobo.llm4j.chat.client;

import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat client response encapsulation
 */
public class ChatClientResponse {
    
    private final ChatResponse chatResponse;
    private final Map<String, Object> metadata;
    
    private ChatClientResponse(Builder builder) {
        this.chatResponse = builder.chatResponse;
        this.metadata = builder.metadata;
    }
    
    public ChatResponse chatResponse() {
        return chatResponse;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ChatResponse chatResponse;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder chatResponse(ChatResponse chatResponse) {
            this.chatResponse = chatResponse;
            return this;
        }
        
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public ChatClientResponse build() {
            return new ChatClientResponse(this);
        }
    }
}
