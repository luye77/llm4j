package com.bobo.llm4j.service.factor;

import com.bobo.llm4j.platform.openai.chat.OpenAiChatModel;
import com.bobo.llm4j.platform.openai.embedding.OpenAiEmbeddingModel;
import com.bobo.llm4j.platform.qwen.chat.QwenChatModel;
import com.bobo.llm4j.platform.qwen.embedding.QwenEmbeddingModel;
import com.bobo.llm4j.service.*;
import lombok.Getter;

/**
 * @Author bo
 * @Description AI服务工厂，创建ChatModel和EmbeddingModel
 * @Date 2024/8/7 18:10
 */
@Getter
public class AiService {

    private final Configuration configuration;

    public AiService(Configuration configuration) {
        this.configuration = configuration;
    }

    public ChatModel getChatModel() {
        return new OpenAiChatModel(configuration);
    }

    public EmbeddingModel getEmbeddingModel() {
        return new OpenAiEmbeddingModel(configuration);
    }

    public ChatModel getQwenChatModel() {
        return new QwenChatModel(configuration);
    }

    public EmbeddingModel getQwenEmbeddingModel() {
        return new QwenEmbeddingModel(configuration);
    }

}
