package com.bobo.llm4j.platform.openai.embedding;

import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.OpenAiConfig;

/**
 * OpenAI embedding model.
 */
public class OpenAiEmbeddingModel extends OpenAiCompatibleEmbeddingModel {

    public OpenAiEmbeddingModel(Configuration configuration, String model) {
        super(configuration.getOkHttpClient(),
                getOpenAiConfig(configuration).getApiHost(),
                getOpenAiConfig(configuration).getApiKey(),
                getOpenAiConfig(configuration).getEmbeddingUrl(),
                model);
    }

    private static OpenAiConfig getOpenAiConfig(Configuration configuration) {
        if (configuration == null || configuration.getOpenAiConfig() == null) {
            throw new IllegalArgumentException("OpenAiConfig cannot be null");
        }
        return configuration.getOpenAiConfig();
    }
}

