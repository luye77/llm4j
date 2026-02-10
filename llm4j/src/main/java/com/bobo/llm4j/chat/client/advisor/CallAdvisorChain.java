package com.bobo.llm4j.chat.client.advisor;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.ChatClientResponse;

/**
 * CallAdvisorChain - 同步调用的 Advisor 链
 * <p>
 * 参考 Spring AI 的 CallAdvisorChain 设计
 *
 * @author bobo
 * @since 1.0.0
 */
public interface CallAdvisorChain extends AdvisorChain {

    /**
     * 调用链中的下一个 Advisor
     *
     * @param chatClientRequest 请求对象
     * @return 响应对象
     */
    ChatClientResponse nextCall(ChatClientRequest chatClientRequest);
}
