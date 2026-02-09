package com.bobo.llm4j.tool;

/**
 * Provider for tool callbacks
 */
public interface ToolCallbackProvider {
    
    ToolCallback[] getToolCallbacks();
}
