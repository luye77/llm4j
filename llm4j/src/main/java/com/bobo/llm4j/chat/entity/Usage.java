package com.bobo.llm4j.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用量统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usage {

    /**
     * 提示token数量
     */
    @JsonProperty("prompt_tokens")
    private Integer promptTokens = 0;

    /**
     * 补全token数量
     */
    @JsonProperty("completion_tokens")
    private Integer completionTokens = 0;

    /**
     * 总token数量
     */
    @JsonProperty("total_tokens")
    private Integer totalTokens = 0;
}

