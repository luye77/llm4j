package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;
import com.bobo.llm4j.http.Flux;

/**
 * BaseAdvisor - Advisor 的基础接口
 * <p>
 * 参考 Spring AI 的 BaseAdvisor 设计
 * 提供默认的 adviseCall 和 adviseStream 实现，
 * 子类只需实现 before() 和 after() 方法即可
 *
 * <p>工作流程：
 * <pre>
 * adviseCall() {
 *     request = before(request)     // 请求前处理
 *     response = chain.nextCall()   // 调用链中的下一个 Advisor
 *     return after(response)        // 响应后处理
 * }
 * </pre>
 *
 * @author bobo
 * @since 1.0.0
 */
public interface BaseAdvisor extends CallAdvisor, StreamAdvisor {

    /**
     * 默认的同步调用实现
     * <p>
     * 按顺序执行：before -> 调用链 -> after
     */
    @Override
    default ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, 
                                         CallAdvisorChain callAdvisorChain) {
        if (chatClientRequest == null) {
            throw new IllegalArgumentException("chatClientRequest cannot be null");
        }
        if (callAdvisorChain == null) {
            throw new IllegalArgumentException("callAdvisorChain cannot be null");
        }

        // 1. before: 请求前处理
        ChatClientRequest processedRequest = before(chatClientRequest, callAdvisorChain);

        // 2. 调用链中的下一个 Advisor
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(processedRequest);

        // 3. after: 响应后处理
        return after(chatClientResponse, callAdvisorChain);
    }

    /**
     * 默认的流式调用实现
     * <p>
     * 按顺序执行：before -> 流式调用链 -> after（在流结束时）
     */
    @Override
    default Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                  StreamAdvisorChain streamAdvisorChain) {
        if (chatClientRequest == null) {
            throw new IllegalArgumentException("chatClientRequest cannot be null");
        }
        if (streamAdvisorChain == null) {
            throw new IllegalArgumentException("streamAdvisorChain cannot be null");
        }

        // 1. before: 请求前处理
        ChatClientRequest processedRequest = before(chatClientRequest, streamAdvisorChain);

        // 2. 调用链中的下一个流式 Advisor
        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(processedRequest);

        // 3. after: 在流的最后一个元素时执行
        return responseFlux.map(response -> {
            // 简化实现：对每个响应都调用 after
            // 实际应用中可能需要判断是否是最后一个响应
            return after(response, streamAdvisorChain);
        });
    }

    @Override
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 请求前处理
     * <p>
     * 在调用 ChatModel 之前执行，可以修改请求
     *
     * @param chatClientRequest 原始请求
     * @param advisorChain Advisor 链
     * @return 处理后的请求
     */
    ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain);

    /**
     * 响应后处理
     * <p>
     * 在 ChatModel 返回响应后执行，可以处理响应
     *
     * @param chatClientResponse 响应对象
     * @param advisorChain Advisor 链
     * @return 处理后的响应
     */
    ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain);
}
