package com.bobo.llm4j.tool;

/**
 * Default predicate for tool execution.
 */
public class DefaultToolExecutionEligibilityPredicate implements ToolExecutionEligibilityPredicate {

    @Override
    public boolean isToolExecutionRequired(com.bobo.llm4j.chat.entity.Prompt prompt,
                                           com.bobo.llm4j.chat.entity.ChatResponse chatResponse) {
        return prompt != null
                && Boolean.TRUE.equals(prompt.getInternalToolExecutionEnabled())
                && chatResponse != null
                && chatResponse.hasToolCalls();
    }
}
