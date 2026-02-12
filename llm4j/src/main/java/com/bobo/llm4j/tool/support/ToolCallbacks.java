package com.bobo.llm4j.tool.support;

import com.bobo.llm4j.tool.ToolCallback;
import com.bobo.llm4j.tool.ToolDefinition;
import com.bobo.llm4j.tool.ToolMetadata;
import com.bobo.llm4j.tool.annotation.Tool;
import com.bobo.llm4j.tool.method.MethodToolCallback;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Build tool callbacks from annotated objects.
 */
public final class ToolCallbacks {

    private ToolCallbacks() {
    }

    public static List<ToolCallback> from(Object... sources) {
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        if (sources == null) {
            return callbacks;
        }
        for (Object source : sources) {
            if (source == null) {
                continue;
            }
            callbacks.addAll(fromOne(source));
        }
        return callbacks;
    }

    private static List<ToolCallback> fromOne(Object source) {
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        Method[] methods = source.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool == null) {
                continue;
            }
            String toolName = hasText(tool.name()) ? tool.name() : method.getName();
            String description = hasText(tool.description())
                    ? tool.description()
                    : ToolDefinitionUtils.defaultDescriptionFromName(toolName);

            ToolDefinition definition = ToolDefinition.builder()
                    .name(toolName)
                    .description(description)
                    .inputSchema(ToolDefinitionUtils.buildInputSchema(method.getParameters()))
                    .build();

            Object target = Modifier.isStatic(method.getModifiers()) ? null : source;
            callbacks.add(new MethodToolCallback(definition, ToolMetadata.builder().build(), method, target));
        }
        return callbacks;
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
