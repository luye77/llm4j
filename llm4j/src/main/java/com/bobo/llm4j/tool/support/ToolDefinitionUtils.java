package com.bobo.llm4j.tool.support;

import com.bobo.llm4j.tool.annotation.ToolParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for tool definition and schema generation.
 */
public final class ToolDefinitionUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolDefinitionUtils() {
    }

    public static String defaultDescriptionFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        return "Invoke tool " + name;
    }

    public static String buildInputSchema(Parameter[] parameters) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<String, Object>();
        List<String> required = new ArrayList<String>();

        for (Parameter parameter : parameters) {
            if ("com.bobo.llm4j.tool.ToolContext".equals(parameter.getType().getName())) {
                continue;
            }
            Map<String, Object> attr = new LinkedHashMap<String, Object>();
            attr.put("type", mapJsonType(parameter.getType()));

            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
            if (toolParam != null && toolParam.description() != null && !toolParam.description().trim().isEmpty()) {
                attr.put("description", toolParam.description());
            }

            props.put(parameter.getName(), attr);
            if (toolParam == null || toolParam.required()) {
                required.add(parameter.getName());
            }
        }

        root.put("properties", props);
        root.put("required", required);
        root.put("additionalProperties", false);
        try {
            return MAPPER.writeValueAsString(root);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build tool input schema", e);
        }
    }

    private static String mapJsonType(Class<?> type) {
        if (type == null) {
            return "string";
        }
        if (CharSequence.class.isAssignableFrom(type) || Character.class.equals(type) || char.class.equals(type)) {
            return "string";
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return "boolean";
        }
        if (Number.class.isAssignableFrom(type)
                || byte.class.equals(type)
                || short.class.equals(type)
                || int.class.equals(type)
                || long.class.equals(type)
                || float.class.equals(type)
                || double.class.equals(type)) {
            return "number";
        }
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return "array";
        }
        if (Map.class.isAssignableFrom(type)) {
            return "object";
        }
        return "object";
    }
}
