package com.bobo.llm4j.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime context passed into tool execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolContext {

    private Map<String, Object> context = new HashMap<String, Object>();

    public Map<String, Object> readOnlyContext() {
        return context == null ? Collections.<String, Object>emptyMap() : Collections.unmodifiableMap(context);
    }
}
