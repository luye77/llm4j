package com.bobo.llm4j.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.bobo.llm4j.platform.openai.chat.enums.MessageType;
import com.bobo.llm4j.platform.openai.tool.ToolCall;
import lombok.*;

import java.util.List;

/**
 * Message - 消息实体 (对应Spring AI的Message)
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private Media content;
    private String role;
    private String name;
    private String refusal;

    @JsonProperty("reasoning_content")
    private String reasoningContent;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    public Message(String userMessage) {
        this.role = MessageType.USER.getRole();
        this.content = Media.ofText(userMessage);
    }

    public Message(MessageType role, String message) {
        this.role = role.getRole();
        this.content = Media.ofText(message);
    }

    public Message(String role, String message) {
        this.role = role;
        this.content = Media.ofText(message);
    }

    public static Message withSystem(String content) {
        return new Message(MessageType.SYSTEM, content);
    }

    public static Message withUser(String content) {
        return new Message(MessageType.USER, content);
    }

    public static Message withUser(String content, String ...images) {
        return Message.builder()
                .role(MessageType.USER.getRole())
                .content(Media.ofMultiModals(Media.MultiModal.withMultiModal(content, images)))
                .build();
    }

    public static Message withAssistant(String content) {
        return new Message(MessageType.ASSISTANT, content);
    }

    public static Message withAssistant(List<ToolCall> toolCalls) {
        return Message.builder()
                .role(MessageType.ASSISTANT.getRole())
                .toolCalls(toolCalls)
                .build();
    }

    public static Message withAssistant(String content, List<ToolCall> toolCalls) {
        return Message.builder()
                .role(MessageType.ASSISTANT.getRole())
                .content(Media.ofText(content))
                .toolCalls(toolCalls)
                .build();
    }

    public static Message withTool(String content, String toolCallId) {
        return Message.builder()
                .role(MessageType.TOOL.getRole())
                .content(Media.ofText(content))
                .toolCallId(toolCallId)
                .build();
    }
}

