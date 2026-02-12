package com.bobo.llm4j.tool;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;

import java.util.List;

/**
 * Tool calling coordinator.
 */
public interface ToolCallingManager {

    List<ToolDefinition> resolveToolDefinitions(Prompt prompt);

    ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);
}
