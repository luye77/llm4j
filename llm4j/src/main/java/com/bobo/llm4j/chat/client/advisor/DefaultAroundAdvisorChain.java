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
 * DefaultAroundAdvisorChain - 默认的 Advisor 链实现
 * <p>
 * 参考 Spring AI 的责任链模式
 * 按顺序执行所有 Advisor 的 adviseCall 或 adviseStream 方法
 *
 * @author bobo
 * @since 1.0.0
 */
public class DefaultAroundAdvisorChain extends BaseAdvisorChain {
    
    private int currentIndex = 0;
    
    private DefaultAroundAdvisorChain(Builder builder) {
        super(builder.advisors, builder.templateRenderer);
    }
    
    @Override
    public ChatClientResponse nextCall(ChatClientRequest request) {
        // 如果还有 advisor 未执行
        if (currentIndex < advisors.size()) {
            Advisor currentAdvisor = advisors.get(currentIndex);
            currentIndex++;
            
            // 如果是 CallAdvisor（包括 BaseAdvisor），调用 adviseCall
            if (currentAdvisor instanceof CallAdvisor) {
                CallAdvisor callAdvisor = (CallAdvisor) currentAdvisor;
                // adviseCall 内部会调用 before -> nextCall -> after
                return callAdvisor.adviseCall(request, this);
            }
            // 如果是 ChatModelCallAdvisor（最后一个 advisor），直接调用模型
            else if (currentAdvisor instanceof ChatModelCallAdvisor) {
                return ((ChatModelCallAdvisor) currentAdvisor).call(request);
            }
            // 其他类型的 advisor，跳过
            else {
                return nextCall(request);
            }
        }
        
        return ChatClientResponse.builder().build();
    }
    
    @Override
    public Flux<ChatClientResponse> nextStream(ChatClientRequest request) {
        // 如果还有 advisor 未执行
        if (currentIndex < advisors.size()) {
            Advisor currentAdvisor = advisors.get(currentIndex);
            currentIndex++;
            
            // 如果是 StreamAdvisor（包括 BaseAdvisor），调用 adviseStream
            if (currentAdvisor instanceof StreamAdvisor) {
                StreamAdvisor streamAdvisor = (StreamAdvisor) currentAdvisor;
                // adviseStream 内部会调用 before -> nextStream -> after
                return streamAdvisor.adviseStream(request, this);
            }
            // 如果是 ChatModelStreamAdvisor（最后一个 advisor），直接调用模型
            else if (currentAdvisor instanceof ChatModelStreamAdvisor) {
                return ((ChatModelStreamAdvisor) currentAdvisor).stream(request);
            }
            // 其他类型的 advisor，跳过
            else {
                return nextStream(request);
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
