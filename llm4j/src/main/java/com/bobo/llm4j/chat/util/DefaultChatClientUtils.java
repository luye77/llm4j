package com.bobo.llm4j.chat.util;

import com.bobo.llm4j.chat.client.ChatClientRequest;
import com.bobo.llm4j.chat.client.DefaultChatClient;
import com.bobo.llm4j.chat.prompt.ChatOptions;
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
            // Copy options to prompt
        }
        
        return builder.build();
    }
}
