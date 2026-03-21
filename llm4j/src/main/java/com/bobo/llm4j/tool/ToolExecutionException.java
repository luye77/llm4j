package com.bobo.llm4j.tool;

import lombok.Getter;

/**
 * Exception thrown when a tool execution fails.
 */
@Getter
public class ToolExecutionException extends RuntimeException {

    private final ToolDefinition toolDefinition;

    public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
        super(cause);
        this.toolDefinition = toolDefinition;
    }

    public ToolExecutionException(ToolDefinition toolDefinition, String message) {
        super(message);
        this.toolDefinition = toolDefinition;
    }

    public ToolExecutionException(String message) {
        super(message);
        this.toolDefinition = null;
    }

}
