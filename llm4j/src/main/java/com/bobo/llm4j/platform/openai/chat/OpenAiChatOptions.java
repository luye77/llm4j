package com.bobo.llm4j.platform.openai.chat;

import com.bobo.llm4j.chat.client.ChatOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAiChatOptions - OpenAI specific chat model options
 * <p>
 * This class extends the base ChatOptions interface with OpenAI-specific options.
 * It supports all portable options plus OpenAI-specific features like:
 * <ul>
 *   <li>Response format (JSON mode)</li>
 *   <li>Logit bias for token steering</li>
 *   <li>Seed for reproducible outputs</li>
 *   <li>Logprobs for debugging</li>
 *   <li>User identifier for tracking</li>
 * </ul>
 * </p>
 * 
 * @see ChatOptions
 * @see <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI Chat API</a>
 * @author bobo
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiChatOptions implements ChatOptions {
    
    // ========== Portable ChatOptions Fields ==========
    
    /**
     * Model name (e.g., "gpt-4", "gpt-3.5-turbo")
     */
    @JsonProperty("model")
    private String model;
    
    /**
     * Frequency penalty [-2.0, 2.0]
     */
    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty;
    
    /**
     * Maximum tokens to generate
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * Presence penalty [-2.0, 2.0]
     */
    @JsonProperty("presence_penalty")
    private Float presencePenalty;
    
    /**
     * Stop sequences (up to 4)
     */
    @JsonProperty("stop")
    private List<String> stopSequences;
    
    /**
     * Sampling temperature [0.0, 2.0]
     */
    @JsonProperty("temperature")
    private Float temperature;
    
    /**
     * Top-K sampling (not used by OpenAI, kept for interface compatibility)
     */
    @JsonProperty("top_k")
    private Integer topK;
    
    /**
     * Top-P (nucleus) sampling [0.0, 1.0]
     */
    @JsonProperty("top_p")
    private Float topP;
    
    // ========== OpenAI-Specific Fields ==========
    
    /**
     * Logit bias for token steering
     * <p>
     * Maps token IDs to bias values [-100, 100].
     * Positive values increase likelihood, negative values decrease likelihood.
     * </p>
     */
    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;
    
    /**
     * Seed for reproducible outputs
     * <p>
     * If specified along with system_fingerprint, returns deterministic outputs.
     * Note: This is a beta feature and not guaranteed to be deterministic.
     * </p>
     */
    @JsonProperty("seed")
    private Integer seed;
    
    /**
     * User identifier for tracking and abuse detection
     */
    @JsonProperty("user")
    private String user;
    
    /**
     * Response format specification
     * <p>
     * Used to enable JSON mode or other structured outputs.
     * Example: new ResponseFormat("json_object")
     * </p>
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    /**
     * Whether to return log probabilities
     */
    @JsonProperty("logprobs")
    private Boolean logprobs;
    
    /**
     * Number of most likely tokens to return log probabilities for (0-20)
     * <p>
     * Only applicable if logprobs is true.
     * </p>
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;
    
    /**
     * Number of chat completions to generate (1-128)
     */
    @JsonProperty("n")
    private Integer n;
    
    /**
     * Enable parallel tool calling
     */
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;
    
    /**
     * Service tier for request routing
     */
    @JsonProperty("service_tier")
    private String serviceTier;
    
    @Override
    public ChatOptions copy() {
        return this.toBuilder().build();
    }
    
    @Override
    public ChatOptions merge(ChatOptions other) {
        if (other == null) {
            return this.copy();
        }
        
        if (!(other instanceof OpenAiChatOptions)) {
            throw new IllegalArgumentException(
                "Cannot merge OpenAiChatOptions with " + other.getClass().getName()
            );
        }
        
        OpenAiChatOptions otherOptions = (OpenAiChatOptions) other;
        
        return OpenAiChatOptions.builder()
            // Portable options: prefer other's values
            .model(otherOptions.model != null ? otherOptions.model : this.model)
            .frequencyPenalty(otherOptions.frequencyPenalty != null ? 
                otherOptions.frequencyPenalty : this.frequencyPenalty)
            .maxTokens(otherOptions.maxTokens != null ? 
                otherOptions.maxTokens : this.maxTokens)
            .presencePenalty(otherOptions.presencePenalty != null ? 
                otherOptions.presencePenalty : this.presencePenalty)
            .stopSequences(otherOptions.stopSequences != null ? 
                otherOptions.stopSequences : this.stopSequences)
            .temperature(otherOptions.temperature != null ? 
                otherOptions.temperature : this.temperature)
            .topK(otherOptions.topK != null ? otherOptions.topK : this.topK)
            .topP(otherOptions.topP != null ? otherOptions.topP : this.topP)
            
            // OpenAI-specific options
            .logitBias(otherOptions.logitBias != null ? 
                otherOptions.logitBias : this.logitBias)
            .seed(otherOptions.seed != null ? otherOptions.seed : this.seed)
            .user(otherOptions.user != null ? otherOptions.user : this.user)
            .responseFormat(otherOptions.responseFormat != null ? 
                otherOptions.responseFormat : this.responseFormat)
            .logprobs(otherOptions.logprobs != null ? 
                otherOptions.logprobs : this.logprobs)
            .topLogprobs(otherOptions.topLogprobs != null ? 
                otherOptions.topLogprobs : this.topLogprobs)
            .n(otherOptions.n != null ? otherOptions.n : this.n)
            .parallelToolCalls(otherOptions.parallelToolCalls != null ? 
                otherOptions.parallelToolCalls : this.parallelToolCalls)
            .serviceTier(otherOptions.serviceTier != null ? 
                otherOptions.serviceTier : this.serviceTier)
            .build();
    }
    
    /**
     * ResponseFormat - Specifies the format of the model's output
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseFormat {
        
        /**
         * Response format type
         * <p>
         * Possible values:
         * <ul>
         *   <li>"text" - Default, plain text responses</li>
         *   <li>"json_object" - Ensures output is valid JSON</li>
         * </ul>
         * </p>
         */
        @JsonProperty("type")
        private String type;
        
        /**
         * Convenience constructor for JSON mode
         */
        public static ResponseFormat jsonObject() {
            return ResponseFormat.builder().type("json_object").build();
        }
        
        /**
         * Convenience constructor for text mode
         */
        public static ResponseFormat text() {
            return ResponseFormat.builder().type("text").build();
        }
    }
    
    /**
     * Create a builder with commonly used defaults for OpenAI GPT-3.5-turbo
     */
    public static OpenAiChatOptionsBuilder defaults() {
        return builder()
            .model("gpt-3.5-turbo")
            .temperature(0.7f)
            .maxTokens(2000);
    }
    
    /**
     * Create a builder for GPT-4
     */
    public static OpenAiChatOptionsBuilder gpt4() {
        return builder()
            .model("gpt-4")
            .temperature(0.7f)
            .maxTokens(2000);
    }
    
    /**
     * Create a builder for GPT-4 Turbo
     */
    public static OpenAiChatOptionsBuilder gpt4Turbo() {
        return builder()
            .model("gpt-4-turbo-preview")
            .temperature(0.7f)
            .maxTokens(4000);
    }
}
