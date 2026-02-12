package com.bobo.llm4j.tool;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Message;
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.ToolCall;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default ToolCallingManager implementation.
 */
@Slf4j
public class DefaultToolCallingManager implements ToolCallingManager {

    @Override
    public List<ToolDefinition> resolveToolDefinitions(Prompt prompt) {
        List<ToolCallback> callbacks = resolveCallbacks(prompt);
        List<ToolDefinition> definitions = new ArrayList<ToolDefinition>();
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null) {
                continue;
            }
            definitions.add(callback.getToolDefinition());
        }
        return definitions;
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        if (prompt == null || chatResponse == null) {
            throw new IllegalArgumentException("prompt and chatResponse cannot be null");
        }

        Message assistantMessage = extractAssistantToolCallMessage(chatResponse);
        if (assistantMessage == null || assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().isEmpty()) {
            throw new IllegalStateException("No tool call found in chat response");
        }

        Map<String, ToolCallback> callbackIndex = buildCallbackIndex(resolveCallbacks(prompt));
        List<Message> nextHistory = new ArrayList<Message>(prompt.getMessages());
        nextHistory.add(assistantMessage);

        ToolContext toolContext = new ToolContext(prompt.getToolContext());
        for (ToolCall toolCall : assistantMessage.getToolCalls()) {
            String toolName = toolCall != null && toolCall.getFunction() != null ? toolCall.getFunction().getName() : null;
            String arguments = toolCall != null && toolCall.getFunction() != null ? toolCall.getFunction().getArguments() : "{}";
            if (arguments == null || arguments.trim().isEmpty()) {
                arguments = "{}";
            }
            ToolCallback callback = callbackIndex.get(toolName);
            if (callback == null) {
                throw new IllegalStateException("No tool callback found for tool name: " + toolName);
            }
            String toolResult = callback.call(arguments, toolContext);
            nextHistory.add(Message.withTool(toolCall.getId(), toolResult));
        }

        return ToolExecutionResult.builder().conversationHistory(nextHistory).build();
    }

    private List<ToolCallback> resolveCallbacks(Prompt prompt) {
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        if (prompt.getToolCallbacks() != null) {
            callbacks.addAll(prompt.getToolCallbacks());
        }
        // Tool names resolution can be plugged later if registry is needed.
        return callbacks;
    }

    private Map<String, ToolCallback> buildCallbackIndex(List<ToolCallback> callbacks) {
        Map<String, ToolCallback> callbackIndex = new LinkedHashMap<String, ToolCallback>();
        for (ToolCallback callback : callbacks) {
            if (callback == null || callback.getToolDefinition() == null || callback.getToolDefinition().getName() == null) {
                continue;
            }
            String name = callback.getToolDefinition().getName();
            if (callbackIndex.containsKey(name)) {
                throw new IllegalStateException("Duplicate tool callback name: " + name);
            }
            callbackIndex.put(name, callback);
        }
        return callbackIndex;
    }

    private Message extractAssistantToolCallMessage(ChatResponse response) {
        if (response.getGenerations() == null) {
            return null;
        }
        for (int i = 0; i < response.getGenerations().size(); i++) {
            Message message = response.getGenerations().get(i).getMessage();
            if (message != null && message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                return Message.withAssistantToolCalls(
                        message.getContent() == null ? null : message.getContent().getText(),
                        message.getToolCalls()
                );
            }
        }
        return null;
    }
}
