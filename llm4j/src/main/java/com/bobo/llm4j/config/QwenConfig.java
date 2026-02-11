package com.bobo.llm4j.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author bo
 * @Description 通义千问平台配置文件信息 (Qwen, OpenAI兼容模式)
 * @Date 2026/2/2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenConfig {
    /**
     * OpenAI兼容模式基础地址
     */
    @Builder.Default
    private String apiHost = "https://dashscope.aliyuncs.com/";
    @Builder.Default
    private String apiKey = "";
    @Builder.Default
    private String chatCompletionUrl = "compatible-mode/v1/chat/completions";
    @Builder.Default
    private String embeddingUrl = "compatible-mode/v1/embeddings";
    /**
     * 是否使用OpenAI兼容模式
     */
    @Builder.Default
    private boolean compatibleMode = true;
    /**
     * DashScope文本对话接口
     */
    @Builder.Default
    private String dashscopeChatCompletionUrl = "api/v1/services/aigc/text-generation/generation";
    /**
     * DashScope多模态对话接口
     */
    @Builder.Default
    private String dashscopeMultiModalCompletionUrl = "api/v1/services/aigc/multimodal-generation/generation";
    /**
     * DashScope向量接口
     */
    @Builder.Default
    private String dashscopeEmbeddingUrl = "api/v1/services/embeddings/text-embedding/text-embedding";
}
