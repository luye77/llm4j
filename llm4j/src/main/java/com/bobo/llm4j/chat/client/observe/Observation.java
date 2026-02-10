package com.bobo.llm4j.chat.client.observe;

import java.util.function.Supplier;

/**
 * Observation for monitoring operations
 */
public interface Observation {
    
    <T> T observe(Supplier<T> supplier);
    
    Observation parentObservation(Observation parent);
    
    void start();
    
    void error(Throwable error);
    
    void stop();
}
