package com.bobo.llm4j.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Additional metadata for a tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolMetadata {

    @Builder.Default
    private boolean returnDirect = false;
}
