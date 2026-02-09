package com.bobo.llm4j.chat.client;

/**
 * Attributes for chat client context
 */
public enum ChatClientAttributes {
    
    OUTPUT_FORMAT("output_format");
    
    private final String key;
    
    ChatClientAttributes(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
}
