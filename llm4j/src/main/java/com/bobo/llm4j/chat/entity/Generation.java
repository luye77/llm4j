package com.bobo.llm4j.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Generation - 模型生成结果 (对应Spring AI的Generation)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Generation {
    private Integer index;

    /**
     * 流式响应中的增量消息
     */
    private Message delta;

    /**
     * 完整消息
     */
    private Message message;

    private Object logprobs;

    /**
     * 停止原因
     * [stop, length, content_filter, tool_calls, insufficient_system_resource]
     */
    @JsonProperty("finish_reason")
    private String finishReason;
}

