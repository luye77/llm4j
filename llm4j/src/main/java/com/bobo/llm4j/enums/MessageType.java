package com.bobo.llm4j.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MessageType - 消息类型枚举 (对应Spring AI的MessageType)
 */
@Getter
@AllArgsConstructor
public enum MessageType {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    ;

    private final String role;
}

