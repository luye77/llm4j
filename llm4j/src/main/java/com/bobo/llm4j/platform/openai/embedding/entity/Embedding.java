package com.bobo.llm4j.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding - 单个Embedding结果 (对应Spring AI的Embedding)
 */
@Data
@Builder
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Embedding {
    /**
     * 结果下标
     */
    private Integer index;

    /**
     * 向量化表征数组
     */
    private List<Float> embedding;

    /**
     * 结果类型
     */
    private String object;
}
