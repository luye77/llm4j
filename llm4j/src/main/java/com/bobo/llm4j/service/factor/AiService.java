package com.bobo.llm4j.service.factor;

import com.bobo.llm4j.platform.openai.chat.OpenAiChatModel;
import com.bobo.llm4j.platform.openai.embedding.OpenAiEmbeddingModel;
import com.bobo.llm4j.service.*;

/**
 * @Author cly
 * @Description AI服务工厂，创建ChatModel和EmbeddingModel
 * @Date 2024/8/7 18:10
 */
public class AiService {

    private final Configuration configuration;

    public AiService(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ChatModel getChatModel() {
        return new OpenAiChatModel(configuration);
    }

    public EmbeddingModel getEmbeddingModel() {
        return new OpenAiEmbeddingModel(configuration);
    }

}
