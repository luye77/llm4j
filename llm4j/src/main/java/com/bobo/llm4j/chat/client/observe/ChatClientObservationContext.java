package com.bobo.llm4j.chat.client.observe;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.advisor.Advisor;
import lombok.Getter;

import java.util.List;

/**
 * Context for chat client observations
 */
@Getter
public class ChatClientObservationContext {
    
    private final ChatClientRequest request;
    private final List<Advisor> advisors;
    private final boolean stream;
    private final String format;
    
    private ChatClientObservationContext(Builder builder) {
        this.request = builder.request;
        this.advisors = builder.advisors;
        this.stream = builder.stream;
        this.format = builder.format;
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ChatClientRequest request;
        private List<Advisor> advisors;
        private boolean stream;
        private String format;
        
        public Builder request(ChatClientRequest request) {
            this.request = request;
            return this;
        }
        
        public Builder advisors(List<Advisor> advisors) {
            this.advisors = advisors;
            return this;
        }
        
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public ChatClientObservationContext build() {
            return new ChatClientObservationContext(this);
        }
    }
}
