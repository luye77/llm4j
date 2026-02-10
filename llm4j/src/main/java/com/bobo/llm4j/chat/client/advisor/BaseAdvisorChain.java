package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.http.Flux;
import com.bobo.llm4j.template.TemplateRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * BaseAdvisorChain - Advisor 链的基类
 * <p>
 * 参考 Spring AI 的 Advisor 链设计
 * 实现 CallAdvisorChain 和 StreamAdvisorChain 接口
 *
 * @author bobo
 * @since 1.0.0
 */
public abstract class BaseAdvisorChain implements CallAdvisorChain, StreamAdvisorChain {
    
    protected final List<Advisor> advisors;
    protected final TemplateRenderer templateRenderer;
    
    protected BaseAdvisorChain(List<Advisor> advisors, TemplateRenderer templateRenderer) {
        this.advisors = advisors != null ? advisors : new ArrayList<>();
        this.templateRenderer = templateRenderer;
    }
    
    /**
     * 获取 Call 操作的 Advisors
     */
    public List<Advisor> getCallAdvisors() {
        return advisors;
    }
    
    /**
     * 获取 Stream 操作的 Advisors
     */
    public List<Advisor> getStreamAdvisors() {
        return advisors;
    }
}
