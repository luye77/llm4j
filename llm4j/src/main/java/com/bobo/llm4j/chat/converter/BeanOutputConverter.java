package com.bobo.llm4j.chat.converter;

import com.bobo.llm4j.chat.client.StructuredOutputConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converter for converting string output to Java beans
 */
public class BeanOutputConverter<T> implements StructuredOutputConverter<T> {
    
    private final Class<T> type;
    private final ParameterizedTypeReference<T> typeReference;
    private final ObjectMapper objectMapper;
    
    public BeanOutputConverter(Class<T> type) {
        this.type = type;
        this.typeReference = null;
        this.objectMapper = new ObjectMapper();
    }
    
    public BeanOutputConverter(ParameterizedTypeReference<T> typeReference) {
        this.type = null;
        this.typeReference = typeReference;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public T convert(String content) {
        try {
            if (type != null) {
                if (type == String.class) {
                    return (T) content;
                }
                return objectMapper.readValue(content, type);
            } else if (typeReference != null) {
                return objectMapper.readValue(content, objectMapper.constructType(typeReference.getType()));
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert output to " + 
                (type != null ? type.getName() : "type"), e);
        }
    }
    
    @Override
    public String getFormat() {
        return "json";
    }
}
