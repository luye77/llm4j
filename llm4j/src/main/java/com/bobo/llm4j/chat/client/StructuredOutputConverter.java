package com.bobo.llm4j.chat.client;

/**
 * Converter for structured output
 */
public interface StructuredOutputConverter<T> {
    
    /**
     * Convert content string to typed object
     */
    T convert(String content);
    
    /**
     * Get the output format
     */
    String getFormat();
}
