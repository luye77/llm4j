package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.http.Flux;

/**
 * StreamAdvisor - 流式调用的 Advisor 接口
 * <p>
 * 参考 Spring AI 的 StreamAdvisor 设计
 * 用于拦截和处理流式的 ChatModel 调用
 *
 * @author bobo
 * @since 1.0.0
 */
public interface StreamAdvisor extends Advisor {

    /**
     * 拦截流式调用
     * <p>
     * 在流式调用 ChatModel 之前和之后执行自定义逻辑
     *
     * @param chatClientRequest 请求对象
     * @param streamAdvisorChain Advisor 链，用于调用下一个 Advisor
     * @return 流式响应
     */
    Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain);
}
