package com.bobo.llm4j.tool;

/**
 * Executable tool callback.
 */
public interface ToolCallback {

    ToolDefinition getToolDefinition();

    default ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    String call(String toolInput);

    default String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }
}
