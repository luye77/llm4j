package com.bobo.llm4j.chat.model;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.http.Flux;

/**
 * StreamingChatModel interface - Spring AI compatible streaming chat model interface
 * <p>
 * This interface extends the base ChatModel with streaming capabilities.
 * Models implementing this interface can return responses as reactive streams,
 * enabling real-time processing of generated content as it's produced by the AI model.
 * </p>
 * 
 * <p>Design principles (following Spring AI):</p>
 * <ul>
 *   <li>Reactive streaming using Flux API for Server-Sent Events (SSE)</li>
 *   <li>Supports both String and Prompt inputs for flexibility</li>
 *   <li>Non-blocking, asynchronous response processing</li>
 *   <li>Compatible with reactive programming models</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * StreamingChatModel model = new OpenAiChatModel(config);
 * 
 * Flux<ChatResponse> stream = model.stream(
 *     new Prompt(List.of(new UserMessage("Tell me a story")))
 * );
 * 
 * stream.subscribe(
 *     response -> System.out.print(extractContent(response)),
 *     error -> System.err.println("Error: " + error),
 *     () -> System.out.println("\nDone!")
 * );
 * }</pre>
 * 
 * @see ChatModel
 * @see Flux
 * @see ChatResponse
 * @author bobo
 * @since 1.0.0
 */
public interface StreamingChatModel {

    /**
     * Stream the AI model response as a reactive Flux.
     * <p>
     * This method sends a prompt to the AI model and returns a reactive stream
     * of ChatResponse objects. Each response represents a chunk of the generated
     * content, allowing for real-time processing as the model generates output.
     * </p>
     * 
     * <p>The stream will emit:</p>
     * <ul>
     *   <li>Multiple ChatResponse objects containing delta (incremental) content</li>
     *   <li>Complete event when the generation is finished</li>
     *   <li>Error event if something goes wrong</li>
     * </ul>
     * 
     * @param prompt the prompt containing messages and optional runtime options
     * @return a Flux stream of ChatResponse objects
     * @throws Exception if an error occurs during the API call setup
     */
    Flux<ChatResponse> stream(Prompt prompt) throws Exception;

    /**
     * Simplified stream method that accepts a plain text message.
     * <p>
     * This is a convenience method for simple streaming use cases. It creates
     * a basic prompt from the message string and calls {@link #stream(Prompt)}.
     * </p>
     * 
     * <p>Default implementation:</p>
     * <pre>{@code
     * default Flux<ChatResponse> stream(String message) throws Exception {
     *     Prompt prompt = new Prompt(List.of(
     *         Message.builder()
     *             .role("user")
     *             .content(Message.Content.builder().text(message).build())
     *             .build()
     *     ));
     *     return stream(prompt);
     * }
     * }</pre>
     * 
     * @param message the user message text
     * @return a Flux stream of ChatResponse objects
     * @throws Exception if an error occurs during the API call setup
     */
    default Flux<ChatResponse> stream(String message) throws Exception {
        throw new UnsupportedOperationException("String message streaming is not implemented");
    }
}
