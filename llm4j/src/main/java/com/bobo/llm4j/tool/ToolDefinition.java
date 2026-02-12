package com.bobo.llm4j.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool definition exposed to the model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    private String name;

    private String description;

    /**
     * JSON schema string.
     */
    private String inputSchema;
}
