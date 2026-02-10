package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;

/**
 * ChatModel interface - Spring AI compatible chat model interface
 * <p>
 * This interface provides a unified API for interacting with various AI chat models.
 * Implementations should handle both synchronous and streaming chat operations.
 * </p>
 * 
 * <p>Design principles (following Spring AI):</p>
 * <ul>
 *   <li>Simple and portable interface for switching between AI models</li>
 *   <li>Configuration (apiKey, baseUrl) injected via constructor</li>
 *   <li>Runtime options merged with default options via Prompt</li>
 *   <li>Streaming support using reactive Flux API</li>
 * </ul>
 * 
 * @see ChatOptions
 * @see Prompt
 * @see ChatResponse
 * @see StreamingChatModel
 */
public interface ChatModel extends StreamingChatModel {

    /**
     * Synchronously call the AI model with a prompt and return the response.
     * <p>
     * This method sends a prompt to the AI model and waits for the complete response.
     * Runtime options in the prompt will be merged with default options configured
     * in the implementation.
     * </p>
     * 
     * @param prompt the prompt containing messages and optional runtime options
     * @return the complete chat response from the model
     * @throws Exception if an error occurs during the API call
     */
    ChatResponse call(Prompt prompt) throws Exception;

    /**
     * Simplified call method that accepts a plain text message.
     * <p>
     * This is a convenience method for simple use cases. It creates a basic
     * prompt from the message string.
     * </p>
     * 
     * @param message the user message text
     * @return the complete chat response from the model
     * @throws Exception if an error occurs during the API call
     */
    default ChatResponse call(String message) throws Exception {
        throw new UnsupportedOperationException("String message call is not supported");
    }

    /**
     * Get the default options configured for this model.
     * <p>
     * These options can be overridden at runtime by providing options in the Prompt.
     * </p>
     * 
     * @return the default ChatOptions, or null if none configured
     */
    default ChatOptions getDefaultOptions() {
        return null;
    }
}
