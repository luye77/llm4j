package com.bobo.llm4j.tool;

/**
 * Tool callback for function calling
 */
public interface ToolCallback {
    
    String getName();
    
    String getDescription();
    
    String call(String arguments);
}
