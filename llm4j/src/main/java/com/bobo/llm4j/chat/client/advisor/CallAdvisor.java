package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;

/**
 * CallAdvisor - 同步调用的 Advisor 接口
 * <p>
 * 参考 Spring AI 的 CallAdvisor 设计
 * 用于拦截和处理同步的 ChatModel 调用
 *
 * @author bobo
 * @since 1.0.0
 */
public interface CallAdvisor extends Advisor {

    /**
     * 拦截同步调用
     * <p>
     * 在调用 ChatModel 之前和之后执行自定义逻辑
     *
     * @param chatClientRequest 请求对象
     * @param callAdvisorChain Advisor 链，用于调用下一个 Advisor
     * @return 处理后的响应
     */
    ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain);
}
