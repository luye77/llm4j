package com.bobo.llm4j.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import lombok.*;

import java.util.*;

/**
 * Prompt - 对话请求实体 (对应Spring AI的Prompt)
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Prompt {

    /**
     * 对话模型
     */
    @NonNull
    private String model;

    /**
     * 消息内容
     */
    @NonNull
    @Singular
    private List<Message> messages;

    /**
     * 是否流式输出
     */
    @Builder.Default
    private Boolean stream = false;

    /**
     * 流式输出选项
     */
    @JsonProperty("stream_options")
    private StreamOptions streamOptions;

    /**
     * 频率惩罚
     */
    @Builder.Default
    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty = 0f;

    /**
     * 采样温度
     */
    @Builder.Default
    private Float temperature = 1f;

    /**
     * Top-P采样
     */
    @Builder.Default
    @JsonProperty("top_p")
    private Float topP = 1f;

    /**
     * 最大token数（已弃用）
     */
    @Deprecated
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 最大补全token数
     */
    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    /**
     * 工具定义列表
     */
    private List<ToolDefinition> tools;

    /**
     * 函数名称列表（辅助属性）
     */
    @JsonIgnore
    private List<String> functions;

    /**
     * 工具选择策略
     */
    @JsonProperty("tool_choice")
    private String toolChoice;

    /**
     * 是否并行调用工具
     */
    @Builder.Default
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls = true;

    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private Object responseFormat;

    private String user;

    @Builder.Default
    private Integer n = 1;

    /**
     * 停止词
     */
    private List<String> stop;

    /**
     * 存在惩罚
     */
    @Builder.Default
    @JsonProperty("presence_penalty")
    private Float presencePenalty = 0f;

    @JsonProperty("logit_bias")
    private Map logitBias;

    @Builder.Default
    private Boolean logprobs = false;

    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("parameters")
    @Singular
    private Map<String, Object> parameters;


    public static class PromptBuilder {
        private List<String> functions;

        public Prompt.PromptBuilder functions(String... functions){
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            this.functions.addAll(Arrays.asList(functions));
            return this;
        }

        public Prompt.PromptBuilder functions(List<String> functions){
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            if (functions != null) {
                this.functions.addAll(functions);
            }
            return this;
        }
    }
}

