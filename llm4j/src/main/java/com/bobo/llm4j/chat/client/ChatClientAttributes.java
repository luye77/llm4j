package com.bobo.llm4j.chat.client;

import lombok.Getter;

/**
 * Attributes for chat client context
 */
@Getter
public enum ChatClientAttributes {
    
    OUTPUT_FORMAT("output_format");
    
    private final String key;
    
    ChatClientAttributes(String key) {
        this.key = key;
    }

}
