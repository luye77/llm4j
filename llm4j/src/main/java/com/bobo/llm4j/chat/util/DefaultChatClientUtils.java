package com.bobo.llm4j.chat.util;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.DefaultChatClient;
import com.bobo.llm4j.chat.client.ChatOptions;
import com.bobo.llm4j.chat.client.ToolCallingChatOptions;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Utility methods for DefaultChatClient
 */
public class DefaultChatClientUtils {
    
    /**
     * Convert ChatClientRequestSpec to ChatClientRequest
     */
    public static ChatClientRequest toChatClientRequest(DefaultChatClient.DefaultChatClientRequestSpec spec) {
        return new ChatClientRequest(
            spec.getMessages(),
            spec.getChatOptions(),
            new HashMap<>(),
            spec.getMedia()
        );
    }
    
    /**
     * Convert ChatClientRequest to Prompt
     */
    public static Prompt toPrompt(ChatClientRequest request) {
        // Build messages list
        List<Message> messages = new ArrayList<>();
        
        if (request.getMessages() != null) {
            messages.addAll(request.getMessages());
        }
        
        // Build prompt
        Prompt.PromptBuilder builder = Prompt.builder()
                .model("default")
                .messages(messages);
        
        // Add options if available
        if (request.getOptions() != null) {
            ChatOptions options = (ChatOptions) request.getOptions();
            if (options.getModel() != null) {
                builder.model(options.getModel());
            }
            if (options.getFrequencyPenalty() != null) {
                builder.frequencyPenalty(options.getFrequencyPenalty());
            }
            if (options.getMaxTokens() != null) {
                builder.maxCompletionTokens(options.getMaxTokens());
            }
            if (options.getPresencePenalty() != null) {
                builder.presencePenalty(options.getPresencePenalty());
            }
            if (options.getStopSequences() != null) {
                builder.stop(options.getStopSequences());
            }
            if (options.getTemperature() != null) {
                builder.temperature(options.getTemperature());
            }
            if (options.getTopP() != null) {
                builder.topP(options.getTopP());
            }
            if (options instanceof ToolCallingChatOptions) {
                ToolCallingChatOptions toolOptions = (ToolCallingChatOptions) options;
                builder.toolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(
                        toolOptions.getToolCallbacks(), new ArrayList<com.bobo.llm4j.tool.ToolCallback>()));
                builder.toolNames(ToolCallingChatOptions.mergeToolNames(
                        toolOptions.getToolNames(), new java.util.HashSet<String>()));
                builder.toolContext(ToolCallingChatOptions.mergeToolContext(
                        toolOptions.getToolContext(), new HashMap<String, Object>()));
                builder.internalToolExecutionEnabled(toolOptions.getInternalToolExecutionEnabled() != null
                        ? toolOptions.getInternalToolExecutionEnabled()
                        : ToolCallingChatOptions.DEFAULT_INTERNAL_TOOL_EXECUTION_ENABLED);
            }
        }
        
        return builder.build();
    }
}
