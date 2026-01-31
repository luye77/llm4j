package com.bobo.llm4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ToolParam - 工具参数注解 (对应Spring AI的参数描述)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ToolParam {
    String description();
    boolean required() default true;
}

