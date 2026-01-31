package com.bobo.llm4j.utils;

import com.alibaba.fastjson2.JSON;
import com.bobo.llm4j.annotation.Tool;
import com.bobo.llm4j.annotation.ToolParam;
import com.bobo.llm4j.annotation.ToolRequest;
import com.bobo.llm4j.platform.openai.tool.ToolDefinition;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ToolUtil - 工具管理器
 */
public class ToolUtil {

    private static final Logger log = LoggerFactory.getLogger(ToolUtil.class);

    private static final Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(""))
            .setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated));

    public static final Map<String, ToolDefinition> toolEntityMap = new ConcurrentHashMap<>();
    public static final Map<String, Class<?>> toolClassMap = new ConcurrentHashMap<>();
    public static final Map<String, Class<?>> toolRequestMap = new ConcurrentHashMap<>();

    private static volatile boolean initialized = false;

    /**
     * 工具调用入口
     */
    public static String invoke(String functionName, String argument) {
        ensureInitialized();

        try {
            return invokeToolFunction(functionName, argument);
        } catch (Exception e) {
            throw new RuntimeException("工具调用失败: " + functionName + " - " + e.getMessage(), e);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (ToolUtil.class) {
                if (!initialized) {
                    scanTools();
                    initialized = true;
                }
            }
        }
    }

    private static String invokeToolFunction(String functionName, String argument) {
        Class<?> functionClass = toolClassMap.get(functionName);
        Class<?> functionRequestClass = toolRequestMap.get(functionName);

        if (functionClass == null || functionRequestClass == null) {
            throw new RuntimeException("工具未找到: " + functionName);
        }

        log.info("调用工具: {}, 参数: {}", functionName, argument);

        try {
            Method apply = functionClass.getMethod("apply", functionRequestClass);
            Object arg = JSON.parseObject(argument, functionRequestClass);
            Object functionInstance = functionClass.newInstance();
            Object result = apply.invoke(functionInstance, arg);

            String response = JSON.toJSONString(result);
            log.info("工具调用成功: {} -> {}", functionName, response);
            return response;

        } catch (Exception e) {
            log.error("工具调用失败: {}", functionName, e);
            throw new RuntimeException("工具调用失败: " + functionName, e);
        }
    }

    /**
     * 获取工具定义列表
     */
    public static List<ToolDefinition> getAllTools(List<String> functionList) {
        ensureInitialized();

        List<ToolDefinition> tools = new ArrayList<>();

        if (functionList == null || functionList.isEmpty()) {
            return tools;
        }

        log.debug("获取{}个工具", functionList.size());

        for (String functionName : functionList) {
            if (functionName == null || functionName.trim().isEmpty()) {
                continue;
            }

            try {
                ToolDefinition tool = toolEntityMap.get(functionName);
                if (tool == null) {
                    tool = getToolDefinition(functionName);
                    if (tool != null) {
                        toolEntityMap.put(functionName, tool);
                    }
                }

                if (tool != null) {
                    tools.add(tool);
                }
            } catch (Exception e) {
                log.error("获取工具失败: {}", functionName, e);
            }
        }

        log.info("获取工具完成: 请求{}个，成功{}个", functionList.size(), tools.size());
        return tools;
    }

    private static void scanTools() {
        try {
            Set<Class<?>> functionSet = reflections.getTypesAnnotatedWith(Tool.class);

            for (Class<?> functionClass : functionSet) {
                try {
                    Tool tool = functionClass.getAnnotation(Tool.class);
                    if (tool != null) {
                        String functionName = tool.name();
                        toolClassMap.put(functionName, functionClass);

                        Class<?>[] innerClasses = functionClass.getDeclaredClasses();
                        for (Class<?> innerClass : innerClasses) {
                            if (innerClass.getAnnotation(ToolRequest.class) != null) {
                                toolRequestMap.put(functionName, innerClass);
                                break;
                            }
                        }

                        log.debug("注册工具: {}", functionName);
                    }
                } catch (Exception e) {
                    log.error("处理工具类失败: {}", functionClass.getName(), e);
                }
            }

            log.info("扫描工具完成: {}个", toolClassMap.size());
        } catch (Exception e) {
            log.error("扫描工具失败", e);
        }
    }

    public static ToolDefinition getToolDefinition(String functionName) {
        if (functionName == null || functionName.trim().isEmpty()) {
            return null;
        }

        try {
            ToolDefinition.Function functionEntity = getFunctionDefinition(functionName);
            if (functionEntity != null) {
                ToolDefinition tool = new ToolDefinition();
                tool.setType("function");
                tool.setFunction(functionEntity);
                return tool;
            }
        } catch (Exception e) {
            log.error("创建工具定义失败: {}", functionName, e);
        }
        return null;
    }

    public static ToolDefinition.Function getFunctionDefinition(String functionName) {
        if (functionName == null || functionName.trim().isEmpty()) {
            return null;
        }

        try {
            Set<Class<?>> functionSet = reflections.getTypesAnnotatedWith(Tool.class);

            for (Class<?> functionClass : functionSet) {
                try {
                    Tool tool = functionClass.getAnnotation(Tool.class);
                    if (tool != null && tool.name().equals(functionName)) {
                        ToolDefinition.Function function = new ToolDefinition.Function();
                        function.setName(tool.name());
                        function.setDescription(tool.description());

                        setFunctionParameters(function, functionClass);
                        toolClassMap.put(functionName, functionClass);
                        return function;
                    }
                } catch (Exception e) {
                    log.error("处理工具类失败: {}", functionClass.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("获取工具定义失败: {}", functionName, e);
        }
        return null;
    }

    private static void setFunctionParameters(ToolDefinition.Function function, Class<?> functionClass) {
        try {
            Class<?>[] classes = functionClass.getDeclaredClasses();
            Map<String, ToolDefinition.Function.Property> parameters = new HashMap<>();
            List<String> requiredParameters = new ArrayList<>();

            for (Class<?> clazz : classes) {
                ToolRequest request = clazz.getAnnotation(ToolRequest.class);
                if (request == null) {
                    continue;
                }

                toolRequestMap.put(function.getName(), clazz);

                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    ToolParam parameter = field.getAnnotation(ToolParam.class);
                    if (parameter == null) {
                        continue;
                    }

                    ToolDefinition.Function.Property property = createPropertyFromType(field.getType(), parameter.description());
                    parameters.put(field.getName(), property);

                    if (parameter.required()) {
                        requiredParameters.add(field.getName());
                    }
                }
            }

            ToolDefinition.Function.Parameter parameter = new ToolDefinition.Function.Parameter("object", parameters, requiredParameters);
            function.setParameters(parameter);

        } catch (Exception e) {
            log.error("设置工具参数失败: {}", function.getName(), e);
            throw new RuntimeException("设置工具参数失败: " + function.getName(), e);
        }
    }

    private static ToolDefinition.Function.Property createPropertyFromType(Class<?> fieldType, String description) {
        ToolDefinition.Function.Property property = new ToolDefinition.Function.Property();

        if (fieldType.isEnum()) {
            property.setType("string");
            property.setEnumValues(getEnumValues(fieldType));
        } else if (fieldType.equals(String.class)) {
            property.setType("string");
        } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class) ||
                fieldType.equals(long.class) || fieldType.equals(Long.class) ||
                fieldType.equals(short.class) || fieldType.equals(Short.class)) {
            property.setType("integer");
        } else if (fieldType.equals(float.class) || fieldType.equals(Float.class) ||
                fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            property.setType("number");
        } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            property.setType("boolean");
        } else if (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) {
            property.setType("array");

            ToolDefinition.Function.Property items = new ToolDefinition.Function.Property();
            Class<?> elementType = getArrayElementType(fieldType);
            if (elementType != null) {
                if (elementType == String.class) {
                    items.setType("string");
                } else if (elementType == Integer.class || elementType == int.class ||
                           elementType == Long.class || elementType == long.class) {
                    items.setType("integer");
                } else if (elementType == Double.class || elementType == double.class ||
                           elementType == Float.class || elementType == float.class) {
                    items.setType("number");
                } else if (elementType == Boolean.class || elementType == boolean.class) {
                    items.setType("boolean");
                } else {
                    items.setType("object");
                }
            } else {
                items.setType("object");
            }
            property.setItems(items);
        } else if (Map.class.isAssignableFrom(fieldType)) {
            property.setType("object");
        } else {
            property.setType("object");
        }

        property.setDescription(description);
        return property;
    }

    private static Class<?> getArrayElementType(Class<?> arrayType) {
        if (arrayType.isArray()) {
            return arrayType.getComponentType();
        } else if (Collection.class.isAssignableFrom(arrayType)) {
            return null;
        }
        return null;
    }

    private static List<String> getEnumValues(Class<?> enumType) {
        List<String> enumValues = new ArrayList<>();
        for (Object enumConstant : enumType.getEnumConstants()) {
            enumValues.add(enumConstant.toString());
        }
        return enumValues;
    }
}
