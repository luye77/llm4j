package com.bobo.llm4j.http;

import lombok.Getter;

/**
 * Response entity containing both response and entity
 */
@Getter
public class ResponseEntity<R, T> {
    
    private final R response;
    private final T entity;
    
    public ResponseEntity(R response, T entity) {
        this.response = response;
        this.entity = entity;
    }

}
