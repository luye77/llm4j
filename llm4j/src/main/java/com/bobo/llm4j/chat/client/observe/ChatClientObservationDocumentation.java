package com.bobo.llm4j.chat.client.observe;

import java.util.function.Supplier;

/**
 * Documentation for chat client observations
 */
public enum ChatClientObservationDocumentation {
    
    AI_CHAT_CLIENT {
        @Override
        public Observation observation(ChatClientObservationConvention convention,
                                       ChatClientObservationConvention defaultConvention,
                                       Supplier<ChatClientObservationContext> contextSupplier,
                                       ObservationRegistry registry) {
            return new NoOpObservation(contextSupplier.get());
        }
    };
    
    public abstract Observation observation(ChatClientObservationConvention convention,
                                           ChatClientObservationConvention defaultConvention,
                                           Supplier<ChatClientObservationContext> contextSupplier,
                                           ObservationRegistry registry);
    
    private static class NoOpObservation implements Observation {
        private final ChatClientObservationContext context;
        
        public NoOpObservation(ChatClientObservationContext context) {
            this.context = context;
        }
        
        @Override
        public <T> T observe(Supplier<T> supplier) {
            return supplier.get();
        }
        
        @Override
        public Observation parentObservation(Observation parent) {
            return this;
        }
        
        @Override
        public void start() {
        }
        
        @Override
        public void error(Throwable error) {
        }
        
        @Override
        public void stop() {
        }
    }
}
