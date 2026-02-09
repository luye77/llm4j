package com.bobo.llm4j.chat.client;

import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.tool.ToolCallback;
import com.bobo.llm4j.platform.openai.chat.entity.Media;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat client request encapsulation
 */
@Getter
public class ChatClientRequest {
    
    private final List<Message> messages;
    private final ChatOptions options;
    private final List<String> toolNames;
    private final List<ToolCallback> toolCallbacks;
    private final Map<String, Object> context;
    private final List<Media> media;
    
    public ChatClientRequest(List<Message> messages, ChatOptions options, 
                            List<String> toolNames, List<ToolCallback> toolCallbacks,
                            Map<String, Object> context, List<Media> media) {
        this.messages = messages;
        this.options = options;
        this.toolNames = toolNames;
        this.toolCallbacks = toolCallbacks;
        this.context = context != null ? context : new HashMap<>();
        this.media = media;
    }

    public Map<String, Object> context() {
        return context;
    }
    
}
