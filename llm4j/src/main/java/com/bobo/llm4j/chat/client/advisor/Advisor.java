package com.bobo.llm4j.chat.client.advisor;

/**
 * Advisor interface for intercepting chat requests
 */
public interface Advisor {
    
    String getName();
    
    int getOrder();
}
