package com.bobo.llm4j.tool;

import com.bobo.llm4j.chat.entity.ChatResponse;
import com.bobo.llm4j.chat.entity.Prompt;

/**
 * Decide if tool execution is needed.
 */
public interface ToolExecutionEligibilityPredicate {

    boolean isToolExecutionRequired(Prompt prompt, ChatResponse chatResponse);
}
