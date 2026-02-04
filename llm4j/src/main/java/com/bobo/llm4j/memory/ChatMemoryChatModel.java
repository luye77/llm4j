package com.bobo.llm4j.memory;

import com.bobo.llm4j.listener.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Generation;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.chat.enums.MessageType;
import com.bobo.llm4j.service.ChatModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChatMemoryChatModel - 带会话记忆的ChatModel装饰器
 */
public class ChatMemoryChatModel implements ChatModel {

    private final ChatModel delegate;
    private final ChatMemory chatMemory;
    private final String conversationId;

    public ChatMemoryChatModel(ChatModel delegate, ChatMemory chatMemory, String conversationId) {
        this.delegate = delegate;
        this.chatMemory = chatMemory;
        this.conversationId = conversationId;
    }

    @Override
    public ChatResponse call(String baseUrl, String apiKey, Prompt prompt) throws Exception {
        Prompt promptWithMemory = mergePromptWithMemory(prompt);
        ChatResponse response = delegate.call(baseUrl, apiKey, promptWithMemory);
        storeMemory(prompt, response);
        return response;
    }

    @Override
    public ChatResponse call(Prompt prompt) throws Exception {
        Prompt promptWithMemory = mergePromptWithMemory(prompt);
        ChatResponse response = delegate.call(promptWithMemory);
        storeMemory(prompt, response);
        return response;
    }

    @Override
    public void stream(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) throws Exception {
        Prompt promptWithMemory = mergePromptWithMemory(prompt);
        delegate.stream(baseUrl, apiKey, promptWithMemory, handler);
        storeMemory(prompt, buildResponseFromStream(handler));
    }

    @Override
    public void stream(Prompt prompt, StreamingResponseHandler handler) throws Exception {
        Prompt promptWithMemory = mergePromptWithMemory(prompt);
        delegate.stream(promptWithMemory, handler);
        storeMemory(prompt, buildResponseFromStream(handler));
    }

    private Prompt mergePromptWithMemory(Prompt prompt) {
        if (chatMemory == null || isBlank(conversationId)) {
            return prompt;
        }
        List<Message> history = chatMemory.get(conversationId);
        if (history.isEmpty()) {
            return prompt;
        }
        List<Message> merged = new ArrayList<>(history.size() + prompt.getMessages().size());
        merged.addAll(history);
        merged.addAll(prompt.getMessages());
        return prompt.toBuilder()
                .messages(merged)
                .build();
    }

    private void storeMemory(Prompt originalPrompt, ChatResponse response) {
        if (chatMemory == null || isBlank(conversationId) || response == null) {
            return;
        }
        List<Message> toStore = new ArrayList<>();
        for (Message message : safeMessages(originalPrompt)) {
            if (message == null) {
                continue;
            }
            if (MessageType.SYSTEM.getRole().equals(message.getRole())) {
                continue;
            }
            toStore.add(message);
        }
        Message assistant = extractAssistant(response);
        if (assistant != null) {
            toStore.add(assistant);
        }
        if (!toStore.isEmpty()) {
            chatMemory.add(conversationId, toStore);
        }
    }

    private Message extractAssistant(ChatResponse response) {
        if (response == null || response.getGenerations() == null || response.getGenerations().isEmpty()) {
            return null;
        }
        Generation generation = response.getGenerations().get(0);
        return generation == null ? null : generation.getMessage();
    }

    private ChatResponse buildResponseFromStream(StreamingResponseHandler handler) {
        if (handler == null) {
            return null;
        }
        String content = handler.getOutput().toString();
        if (content == null || content.isEmpty()) {
            return null;
        }
        Message assistant = Message.withAssistant(content);
        Generation generation = new Generation();
        generation.setMessage(assistant);
        ChatResponse response = new ChatResponse();
        response.setGenerations(Collections.singletonList(generation));
        return response;
    }

    private List<Message> safeMessages(Prompt prompt) {
        if (prompt == null || prompt.getMessages() == null) {
            return Collections.emptyList();
        }
        return prompt.getMessages();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
