package com.bobo.llm4j.chat.converter;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Reference to a parameterized type
 */
@Getter
public abstract class ParameterizedTypeReference<T> {
    
    private final Type type;
    
    protected ParameterizedTypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("Type parameter not found");
        }
    }

}
