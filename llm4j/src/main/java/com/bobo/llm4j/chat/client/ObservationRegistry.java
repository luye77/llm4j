package com.bobo.llm4j.chat.client;

/**
 * Registry for observations - placeholder for monitoring/observability
 */
public interface ObservationRegistry {
    
    /**
     * No-op implementation
     */
    ObservationRegistry NOOP = new ObservationRegistry() {};
}
