package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.chat.model.ChatModelCallAdvisor;
import com.bobo.llm4j.chat.model.ChatModelStreamAdvisor;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.chat.client.ObservationRegistry;
import com.bobo.llm4j.template.TemplateRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of advisor chain
 */
public class DefaultAroundAdvisorChain extends BaseAdvisorChain {
    
    private DefaultAroundAdvisorChain(Builder builder) {
        super(builder.advisors, builder.templateRenderer);
    }
    
    @Override
    public ChatClientResponse nextCall(ChatClientRequest request) {
        // Apply advisors in order
        for (Advisor advisor : advisors) {
            if (advisor instanceof ChatModelCallAdvisor) {
                return ((ChatModelCallAdvisor) advisor).call(request);
            }
        }
        return ChatClientResponse.builder().build();
    }
    
    @Override
    public Flux<ChatClientResponse> nextStream(ChatClientRequest request) {
        // Apply advisors in order
        for (Advisor advisor : advisors) {
            if (advisor instanceof ChatModelStreamAdvisor) {
                return ((ChatModelStreamAdvisor) advisor).stream(request);
            }
        }
        return Flux.empty();
    }
    
    public static Builder builder(ObservationRegistry observationRegistry) {
        return new Builder(observationRegistry);
    }
    
    public static class Builder {
        private final ObservationRegistry observationRegistry;
        private final List<Advisor> advisors = new ArrayList<>();
        private TemplateRenderer templateRenderer;
        
        public Builder(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
        }
        
        public Builder pushAll(List<Advisor> advisors) {
            if (advisors != null) {
                this.advisors.addAll(advisors);
            }
            return this;
        }
        
        public Builder templateRenderer(TemplateRenderer templateRenderer) {
            this.templateRenderer = templateRenderer;
            return this;
        }
        
        public DefaultAroundAdvisorChain build() {
            return new DefaultAroundAdvisorChain(this);
        }
    }
}
