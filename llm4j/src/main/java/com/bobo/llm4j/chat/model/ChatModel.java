package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;

/**
 * Interface for chat model operations
 * Reference to com.bobo.llm4j.service.ChatModel
 */
public interface ChatModel {

    /**
     * Call the model with a prompt
     */
    ChatResponse call(Prompt prompt) throws Exception;

    /**
     * Get default options for this model
     */
    default ChatOptions getDefaultOptions() {
        return null;
    }
}
