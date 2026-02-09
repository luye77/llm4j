package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.http.Flux;

/**
 * Advisor for streaming chat model responses
 */
public class ChatModelStreamAdvisor implements Advisor {
    
    private final ChatModel chatModel;
    private final int order;
    
    private ChatModelStreamAdvisor(Builder builder) {
        this.chatModel = builder.chatModel;
        this.order = builder.order;
    }
    
    public Flux<ChatClientResponse> stream(ChatClientRequest request) {
        // Streaming is more complex and would require async implementation
        // For now, return empty flux as placeholder
        return Flux.empty();
    }
    
    @Override
    public String getName() {
        return "ChatModelStreamAdvisor";
    }
    
    @Override
    public int getOrder() {
        return order;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ChatModel chatModel;
        private int order = Integer.MAX_VALUE;
        
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }
        
        public Builder order(int order) {
            this.order = order;
            return this;
        }
        
        public ChatModelStreamAdvisor build() {
            return new ChatModelStreamAdvisor(this);
        }
    }
}
