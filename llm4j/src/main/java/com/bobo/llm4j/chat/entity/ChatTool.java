package com.bobo.llm4j.chat.entity;

import com.bobo.llm4j.tool.ToolDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tool descriptor sent in prompt request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Builder.Default
    private String type = "function";

    private Function function;

    public static ChatTool fromDefinition(ToolDefinition toolDefinition) {
        if (toolDefinition == null) {
            return null;
        }
        Object parameters = null;
        try {
            if (toolDefinition.getInputSchema() != null) {
                parameters = MAPPER.readValue(toolDefinition.getInputSchema(), Object.class);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid tool schema for " + toolDefinition.getName(), e);
        }
        return ChatTool.builder()
                .type("function")
                .function(Function.builder()
                        .name(toolDefinition.getName())
                        .description(toolDefinition.getDescription())
                        .parameters(parameters)
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String description;
        @JsonProperty("parameters")
        private Object parameters;
    }
}
