package com.bobo.llm4j.rag.reader.markdown;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Markdown document reader config.
 */
@Data
@Builder
public class MarkdownDocumentReaderConfig {

    @Builder.Default
    private boolean horizontalRuleCreateDocument = true;

    @Builder.Default
    private boolean includeCodeBlock = false;

    @Builder.Default
    private boolean includeBlockquote = false;

    @Builder.Default
    private Map<String, Object> additionalMetadata = new LinkedHashMap<String, Object>();

    public static MarkdownDocumentReaderConfig defaultConfig() {
        return MarkdownDocumentReaderConfig.builder().build();
    }
}

