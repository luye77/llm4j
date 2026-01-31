package com.bobo.llm4j.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ChatResponse - 对话响应实体 (对应Spring AI的ChatResponse)
 */
@Data
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    /**
     * 响应唯一标识符
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 系统指纹
     */
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    /**
     * 生成结果列表
     */
    private List<Generation> generations;

    /**
     * Token使用量
     */
    private Usage usage;

    // 兼容旧的choices字段名
    @JsonProperty("choices")
    public void setChoices(List<Generation> generations) {
        this.generations = generations;
    }

    @JsonProperty("choices")
    public List<Generation> getChoices() {
        return this.generations;
    }
}

