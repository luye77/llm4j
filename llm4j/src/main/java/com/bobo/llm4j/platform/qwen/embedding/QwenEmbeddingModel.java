package com.bobo.llm4j.platform.qwen.embedding;

import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.QwenConfig;
import com.bobo.llm4j.platform.openai.embedding.OpenAiCompatibleEmbeddingModel;

/**
 * Qwen embedding model (OpenAI compatible mode).
 */
public class QwenEmbeddingModel extends OpenAiCompatibleEmbeddingModel {

    public QwenEmbeddingModel(Configuration configuration, String model) {
        super(configuration.getOkHttpClient(),
                getQwenConfig(configuration).getApiHost(),
                getQwenConfig(configuration).getApiKey(),
                getQwenConfig(configuration).getEmbeddingUrl(),
                model);
    }

    private static QwenConfig getQwenConfig(Configuration configuration) {
        if (configuration == null || configuration.getQwenConfig() == null) {
            throw new IllegalArgumentException("QwenConfig cannot be null");
        }
        return configuration.getQwenConfig();
    }
}

