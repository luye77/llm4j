package com.bobo.llm4j;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author bo
 * @Description 通义千问配置文件 (OpenAI兼容模式)
 * @Date 2026/2/2
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "ai.qwen")
public class QwenConfigProperties {
    private String apiHost = "https://dashscope.aliyuncs.com/";
    private String apiKey = "";
    private String chatCompletionUrl = "compatible-mode/v1/chat/completions";
    private String embeddingUrl = "compatible-mode/v1/embeddings";
    private boolean compatibleMode = true;
    private String dashscopeChatCompletionUrl = "api/v1/services/aigc/text-generation/generation";
    private String dashscopeMultiModalCompletionUrl = "api/v1/services/aigc/multimodal-generation/generation";
    private String dashscopeEmbeddingUrl = "api/v1/services/embeddings/text-embedding/text-embedding";
}
