package com.bobo.llm4j.tool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating tool callbacks from objects
 */
public class ToolCallbacks {
    
    public static ToolCallback[] from(Object... toolObjects) {
        List<ToolCallback> callbacks = new ArrayList<>();
        
        for (Object obj : toolObjects) {
            // Simple reflection-based tool callback creation
            for (Method method : obj.getClass().getDeclaredMethods()) {
                if (method.getAnnotation(FunctionTool.class) != null) {
                    callbacks.add(new ReflectionToolCallback(obj, method));
                }
            }
        }
        
        return callbacks.toArray(new ToolCallback[0]);
    }
    
    private static class ReflectionToolCallback implements ToolCallback {
        private final Object target;
        private final Method method;
        
        public ReflectionToolCallback(Object target, Method method) {
            this.target = target;
            this.method = method;
        }
        
        @Override
        public String getName() {
            return method.getName();
        }
        
        @Override
        public String getDescription() {
            FunctionTool annotation = method.getAnnotation(FunctionTool.class);
            return annotation != null ? annotation.value() : "";
        }
        
        @Override
        public String call(String arguments) {
            try {
                Object result = method.invoke(target, arguments);
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke tool: " + getName(), e);
            }
        }
    }
}
