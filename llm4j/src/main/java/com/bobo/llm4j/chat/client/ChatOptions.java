package com.bobo.llm4j.chat.client;

import java.util.List;

/**
 * ChatOptions interface - Spring AI compatible chat options interface
 * <p>
 * This interface defines portable configuration options for AI chat models.
 * These options can be set at three levels:
 * <ol>
 *   <li>Model level (default options when creating the ChatModel)</li>
 *   <li>ChatClient level (default options when building the ChatClient)</li>
 *   <li>Request level (runtime options passed in each Prompt)</li>
 * </ol>
 * Options at each level override the previous level.
 * </p>
 * 
 * <p>Portable options defined in this interface work across most AI models.
 * Model-specific options should be implemented in model-specific subclasses
 * (e.g., OpenAiChatOptions, AnthropicChatOptions).
 * </p>
 * 
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/chatmodel.html#_chat_options">Spring AI Chat Options</a>
 * @author bobo
 * @since 1.0.0
 */
public interface ChatOptions {
    
    /**
     * Get the model name/identifier.
     * <p>
     * Examples: "gpt-4", "gpt-3.5-turbo", "claude-3-opus", "llama-2-70b"
     * </p>
     * 
     * @return the model name, or null if not set
     */
    String getModel();
    
    /**
     * Get the frequency penalty.
     * <p>
     * Penalizes new tokens based on their existing frequency in the text so far.
     * Range: -2.0 to 2.0. Higher values decrease the likelihood of repeating words.
     * </p>
     * 
     * @return the frequency penalty, or null if not set
     */
    Float getFrequencyPenalty();
    
    /**
     * Get the maximum number of tokens to generate.
     * <p>
     * Controls the maximum length of the generated response.
     * Note: The total token count (prompt + completion) cannot exceed the model's context length.
     * </p>
     * 
     * @return the maximum tokens, or null if not set
     */
    Integer getMaxTokens();
    
    /**
     * Get the presence penalty.
     * <p>
     * Penalizes new tokens based on whether they appear in the text so far.
     * Range: -2.0 to 2.0. Higher values increase the likelihood of talking about new topics.
     * </p>
     * 
     * @return the presence penalty, or null if not set
     */
    Float getPresencePenalty();
    
    /**
     * Get the stop sequences.
     * <p>
     * Up to 4 sequences where the API will stop generating further tokens.
     * The returned text will not contain the stop sequence.
     * </p>
     * 
     * @return the stop sequences, or null if not set
     */
    List<String> getStopSequences();
    
    /**
     * Get the temperature.
     * <p>
     * Controls randomness. Range: 0.0 to 2.0.
     * <ul>
     *   <li>Lower values (e.g., 0.2) make output more focused and deterministic</li>
     *   <li>Higher values (e.g., 1.0) make output more random and creative</li>
     * </ul>
     * </p>
     * 
     * @return the temperature, or null if not set
     */
    Float getTemperature();
    
    /**
     * Get the top-K sampling parameter.
     * <p>
     * Limits the sampling pool to the K most likely next tokens.
     * Only applicable to certain models (e.g., PaLM, Gemini).
     * </p>
     * 
     * @return the top-K value, or null if not set
     */
    Integer getTopK();
    
    /**
     * Get the top-P (nucleus) sampling parameter.
     * <p>
     * Limits the sampling pool to tokens with cumulative probability >= P.
     * Range: 0.0 to 1.0. An alternative to temperature sampling.
     * <ul>
     *   <li>0.1 means only tokens comprising the top 10% probability mass are considered</li>
     *   <li>Generally recommended to alter this OR temperature, but not both</li>
     * </ul>
     * </p>
     * 
     * @return the top-P value, or null if not set
     */
    Float getTopP();
    
    /**
     * Create a deep copy of this options object.
     * <p>
     * The copy should be independent of the original, so modifications
     * to the copy don't affect the original.
     * </p>
     * 
     * @return a new ChatOptions instance with the same values
     */
    ChatOptions copy();
    
    /**
     * Merge another options object into this one.
     * <p>
     * Non-null values from the other options override values in this options.
     * Null values in the other options are ignored (this options' values are kept).
     * </p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ChatOptions defaults = OpenAiChatOptions.builder()
     *     .model("gpt-3.5-turbo")
     *     .temperature(0.7f)
     *     .maxTokens(2000)
     *     .build();
     * 
     * ChatOptions runtime = OpenAiChatOptions.builder()
     *     .temperature(0.9f)  // Override temperature
     *     .model("gpt-4")     // Override model
     *     .build();
     * 
     * ChatOptions merged = defaults.merge(runtime);
     * // Result: model="gpt-4", temperature=0.9, maxTokens=2000
     * }</pre>
     * 
     * @param other the options to merge into this one
     * @return a new ChatOptions with merged values
     */
    default ChatOptions merge(ChatOptions other) {
        if (other == null) {
            return this.copy();
        }
        throw new UnsupportedOperationException("Merge not implemented for " + this.getClass().getName());
    }
}
