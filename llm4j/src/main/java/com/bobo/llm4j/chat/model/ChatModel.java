package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.http.Flux;
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
 */
public interface ChatModel {

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
     * Stream the AI model response as a reactive Flux.
     * <p>
     * This method sends a prompt to the AI model and returns a reactive stream
     * of chat responses. This allows for real-time processing of the model's
     * output as it's generated (Server-Sent Events).
     * </p>
     * 
     * @param prompt the prompt containing messages and optional runtime options
     * @return a Flux stream of ChatResponse objects
     * @throws Exception if an error occurs during the API call
     */
    default Flux<ChatResponse> stream(Prompt prompt) throws Exception {
        throw new UnsupportedOperationException("Streaming is not supported by this model");
    }

    /**
     * Simplified stream method that accepts a plain text message.
     * <p>
     * This is a convenience method for simple streaming use cases.
     * </p>
     * 
     * @param message the user message text
     * @return a Flux stream of ChatResponse objects
     * @throws Exception if an error occurs during the API call
     */
    default Flux<ChatResponse> stream(String message) throws Exception {
        throw new UnsupportedOperationException("String message streaming is not supported");
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
