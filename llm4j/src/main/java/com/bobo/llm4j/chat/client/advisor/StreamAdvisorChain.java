package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.http.Flux;

/**
 * StreamAdvisorChain - 流式调用的 Advisor 链
 * <p>
 * 参考 Spring AI 的 StreamAdvisorChain 设计
 *
 * @author bobo
 * @since 1.0.0
 */
public interface StreamAdvisorChain extends AdvisorChain {

    /**
     * 调用链中的下一个流式 Advisor
     *
     * @param chatClientRequest 请求对象
     * @return 流式响应
     */
    Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest);
}
