package com.bobo.llm4j.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author bo
 * @Description 通义千问平台配置文件信息 (Qwen, OpenAI兼容模式)
 * @Date 2026/2/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QwenConfig {
    /**
     * OpenAI兼容模式基础地址
     */
    private String apiHost = "https://dashscope.aliyuncs.com/";
    private String apiKey = "";
    private String chatCompletionUrl = "compatible-mode/v1/chat/completions";
    private String embeddingUrl = "compatible-mode/v1/embeddings";
    /**
     * 是否使用OpenAI兼容模式
     */
    private boolean compatibleMode = true;
    /**
     * DashScope文本对话接口
     */
    private String dashscopeChatCompletionUrl = "api/v1/services/aigc/text-generation/generation";
    /**
     * DashScope多模态对话接口
     */
    private String dashscopeMultiModalCompletionUrl = "api/v1/services/aigc/multimodal-generation/generation";
    /**
     * DashScope向量接口
     */
    private String dashscopeEmbeddingUrl = "api/v1/services/embeddings/text-embedding/text-embedding";
}
