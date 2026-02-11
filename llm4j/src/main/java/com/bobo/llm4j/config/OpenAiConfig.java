package com.bobo.llm4j.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author bo
 * @Description OpenAi平台配置文件信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiConfig {
    @Builder.Default
    private String apiHost = "https://api.openai.com/";
    @Builder.Default
    private String apiKey = "";
    @Builder.Default
    private String chatCompletionUrl = "v1/chat/completions";
    @Builder.Default
    private String embeddingUrl = "v1/embeddings";

}
