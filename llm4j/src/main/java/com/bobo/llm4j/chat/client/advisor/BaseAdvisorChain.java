package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.template.TemplateRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Base advisor chain for processing requests
 */
public abstract class BaseAdvisorChain {
    
    protected final List<Advisor> advisors;
    protected final TemplateRenderer templateRenderer;
    
    protected BaseAdvisorChain(List<Advisor> advisors, TemplateRenderer templateRenderer) {
        this.advisors = advisors != null ? advisors : new ArrayList<>();
        this.templateRenderer = templateRenderer;
    }
    
    /**
     * Process the next call in the chain
     */
    public abstract ChatClientResponse nextCall(ChatClientRequest request);
    
    /**
     * Process the next stream in the chain
     */
    public abstract Flux<ChatClientResponse> nextStream(ChatClientRequest request);
    
    /**
     * Get advisors for call operations
     */
    public List<Advisor> getCallAdvisors() {
        return advisors;
    }
    
    /**
     * Get advisors for stream operations
     */
    public List<Advisor> getStreamAdvisors() {
        return advisors;
    }
}
