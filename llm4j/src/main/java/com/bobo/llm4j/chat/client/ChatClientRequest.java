package com.bobo.llm4j.chat.client;

import com.bobo.llm4j.chat.prompt.ChatOptions;
import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;
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
    private final Map<String, Object> context;
    private final List<Media> media;
    
    public ChatClientRequest(List<Message> messages, ChatOptions options, 
                            Map<String, Object> context, List<Media> media) {
        this.messages = messages;
        this.options = options;
        this.context = context != null ? context : new HashMap<>();
        this.media = media;
    }

    public Map<String, Object> context() {
        return context;
    }
    
}
