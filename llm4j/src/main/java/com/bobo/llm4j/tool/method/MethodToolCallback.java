package com.bobo.llm4j.tool.method;

import com.bobo.llm4j.tool.ToolCallback;
import com.bobo.llm4j.tool.ToolContext;
import com.bobo.llm4j.tool.ToolDefinition;
import com.bobo.llm4j.tool.ToolExecutionException;
import com.bobo.llm4j.tool.ToolMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Method based tool callback.
 */
public class MethodToolCallback implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolDefinition toolDefinition;
    private final ToolMetadata toolMetadata;
    private final Method toolMethod;
    private final Object toolObject;

    public MethodToolCallback(ToolDefinition toolDefinition, ToolMetadata toolMetadata, Method toolMethod, Object toolObject) {
        this.toolDefinition = toolDefinition;
        this.toolMetadata = toolMetadata == null ? ToolMetadata.builder().build() : toolMetadata;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return toolMetadata;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            Map<String, Object> arguments = parseArguments(toolInput);
            Object[] methodArgs = buildMethodArguments(arguments, toolContext);
            Object result = invoke(methodArgs);
            if (result == null) {
                return "";
            }
            if (result instanceof String) {
                return (String) result;
            }
            return MAPPER.writeValueAsString(result);
        }
        catch (ToolExecutionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ToolExecutionException(toolDefinition, e);
        }
    }

    private Map<String, Object> parseArguments(String toolInput) throws Exception {
        if (toolInput == null || toolInput.trim().isEmpty()) {
            return MAPPER.readValue("{}", new TypeReference<Map<String, Object>>() {
            });
        }
        return MAPPER.readValue(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    private Object[] buildMethodArguments(Map<String, Object> toolArguments, ToolContext toolContext) {
        Parameter[] parameters = this.toolMethod.getParameters();
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (ToolContext.class.isAssignableFrom(parameter.getType())) {
                values[i] = toolContext;
                continue;
            }
            Object raw = toolArguments.get(parameter.getName());
            values[i] = toTypedValue(raw, parameter.getType());
        }
        return values;
    }

    private Object toTypedValue(Object raw, Class<?> targetType) {
        if (raw == null) {
            return null;
        }
        return MAPPER.convertValue(raw, targetType);
    }

    private Object invoke(Object[] methodArguments) {
        try {
            if (!Modifier.isPublic(this.toolMethod.getModifiers()) || !this.toolMethod.isAccessible()) {
                this.toolMethod.setAccessible(true);
            }
            return this.toolMethod.invoke(this.toolObject, methodArguments);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access tool method " + this.toolMethod.getName(), e);
        }
        catch (InvocationTargetException e) {
            Throwable target = e.getTargetException() != null ? e.getTargetException() : e;
            throw new ToolExecutionException(toolDefinition, target);
        }
    }
}
