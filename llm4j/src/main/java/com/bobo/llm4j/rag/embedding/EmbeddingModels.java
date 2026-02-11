package com.bobo.llm4j.rag.embedding;

import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.platform.openai.embedding.OpenAiEmbeddingModel;
import com.bobo.llm4j.platform.qwen.embedding.QwenEmbeddingModel;

/**
 * Factory helpers for embedding models.
 */
public final class EmbeddingModels {

    private EmbeddingModels() {
    }

    public static EmbeddingModel create(Configuration configuration, EmbeddingProvider provider,
                                        String qwenModel, String openAiModel) {
        EmbeddingProvider actual = provider == null ? EmbeddingProvider.QWEN : provider;
        if (actual == EmbeddingProvider.OPENAI) {
            return new OpenAiEmbeddingModel(configuration, openAiModel);
        }
        return new QwenEmbeddingModel(configuration, qwenModel);
    }
}

