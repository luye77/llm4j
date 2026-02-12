package com.bobo.llm4j.chat.client;

import com.bobo.llm4j.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat options extension for Tool Calling.
 */
public interface ToolCallingChatOptions extends ChatOptions {

    boolean DEFAULT_INTERNAL_TOOL_EXECUTION_ENABLED = true;

    List<ToolCallback> getToolCallbacks();

    void setToolCallbacks(List<ToolCallback> toolCallbacks);

    Set<String> getToolNames();

    void setToolNames(Set<String> toolNames);

    Map<String, Object> getToolContext();

    void setToolContext(Map<String, Object> toolContext);

    Boolean getInternalToolExecutionEnabled();

    void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled);

    static boolean isInternalToolExecutionEnabled(ChatOptions chatOptions) {
        if (chatOptions instanceof ToolCallingChatOptions) {
            ToolCallingChatOptions options = (ToolCallingChatOptions) chatOptions;
            if (options.getInternalToolExecutionEnabled() != null) {
                return Boolean.TRUE.equals(options.getInternalToolExecutionEnabled());
            }
        }
        return DEFAULT_INTERNAL_TOOL_EXECUTION_ENABLED;
    }

    static Set<String> mergeToolNames(Set<String> runtimeToolNames, Set<String> defaultToolNames) {
        if (runtimeToolNames == null || runtimeToolNames.isEmpty()) {
            return defaultToolNames == null ? new HashSet<String>() : new HashSet<String>(defaultToolNames);
        }
        return new HashSet<String>(runtimeToolNames);
    }

    static List<ToolCallback> mergeToolCallbacks(List<ToolCallback> runtimeToolCallbacks,
                                                 List<ToolCallback> defaultToolCallbacks) {
        if (runtimeToolCallbacks == null || runtimeToolCallbacks.isEmpty()) {
            return defaultToolCallbacks == null
                    ? new ArrayList<ToolCallback>()
                    : new ArrayList<ToolCallback>(defaultToolCallbacks);
        }
        return new ArrayList<ToolCallback>(runtimeToolCallbacks);
    }

    static Map<String, Object> mergeToolContext(Map<String, Object> runtimeToolContext,
                                                Map<String, Object> defaultToolContext) {
        Map<String, Object> merged = new HashMap<String, Object>();
        if (defaultToolContext != null) {
            merged.putAll(defaultToolContext);
        }
        if (runtimeToolContext != null) {
            merged.putAll(runtimeToolContext);
        }
        return merged;
    }
}
