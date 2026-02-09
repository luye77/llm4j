package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import com.bobo.llm4j.chat.util.DefaultChatClientUtils;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;

/**
 * Advisor for calling chat model
 */
public class ChatModelCallAdvisor implements Advisor {
    
    private final ChatModel chatModel;
    private final int order;
    
    private ChatModelCallAdvisor(Builder builder) {
        this.chatModel = builder.chatModel;
        this.order = builder.order;
    }
    
    public ChatClientResponse call(ChatClientRequest request) {
        try {
            // Convert request to prompt
            Prompt prompt = DefaultChatClientUtils.toPrompt(request);
            
            // Call the model
            ChatResponse response = chatModel.call(prompt);
            
            // Wrap response
            return ChatClientResponse.builder()
                    .chatResponse(response)
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("Failed to call chat model", e);
        }
    }
    
    @Override
    public String getName() {
        return "ChatModelCallAdvisor";
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
        
        public ChatModelCallAdvisor build() {
            return new ChatModelCallAdvisor(this);
        }
    }
}
