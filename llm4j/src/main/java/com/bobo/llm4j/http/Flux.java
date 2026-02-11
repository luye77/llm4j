package com.bobo.llm4j.http;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Simple reactive stream implementation (simplified version of Reactor Flux)
 * <p>
 * This is a basic implementation supporting two modes:
 * <ol>
 *   <li>Static mode: Created from existing elements</li>
 *   <li>Streaming mode: Created with a FluxSink for dynamic emission</li>
 * </ol>
 * </p>
 */
public class Flux<T> {
    
    private final List<T> elements;
    private final boolean isStreaming;
    private FluxSink<T> sink;
    
    private Flux(List<T> elements) {
        this.elements = elements;
        this.isStreaming = false;
    }
    
    private Flux(FluxSink<T> sink) {
        this.elements = new CopyOnWriteArrayList<>();
        this.sink = sink;
        this.isStreaming = true;
    }
    
    /**
     * Create a Flux that will emit elements dynamically
     * 
     * @param callback Consumer that receives a FluxSink to emit elements
     * @return a new Flux instance
     */
    public static <T> Flux<T> create(Consumer<FluxSink<T>> callback) {
        FluxSink<T> sink = new FluxSink<>();
        Flux<T> flux = new Flux<>(sink);
        
        // Execute callback in a separate thread
        new Thread(() -> {
            try {
                callback.accept(sink);
            } catch (Exception e) {
                sink.error(e);
            }
        }).start();
        
        return flux;
    }
    
    public static <T> Flux<T> fromIterable(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return new Flux<>(list);
    }
    
    public static <T> Flux<T> empty() {
        return new Flux<>(new ArrayList<>());
    }
    
    public static <T> Flux<T> deferContextual(Function<Object, Flux<T>> supplier) {
        return supplier.apply(null);
    }
    
    public <R> Flux<R> map(Function<T, R> mapper) {
        List<R> mapped = new ArrayList<>();
        for (T element : elements) {
            mapped.add(mapper.apply(element));
        }
        return new Flux<>(mapped);
    }
    
    public <R> Flux<R> mapNotNull(Function<T, R> mapper) {
        List<R> mapped = new ArrayList<>();
        for (T element : elements) {
            R result = mapper.apply(element);
            if (result != null) {
                mapped.add(result);
            }
        }
        return new Flux<>(mapped);
    }
    
    public Flux<T> filter(Predicate<T> predicate) {
        List<T> filtered = new ArrayList<>();
        for (T element : elements) {
            if (predicate.test(element)) {
                filtered.add(element);
            }
        }
        return new Flux<>(filtered);
    }
    
    public Flux<T> doOnError(Consumer<Throwable> onError) {
        // Simple implementation - no error handling
        return this;
    }
    
    public Flux<T> doFinally(Consumer<Object> onFinally) {
        // Simple implementation
        return this;
    }
    
    public Flux<T> contextWrite(Function<Object, Object> contextWriter) {
        // Simple implementation
        return this;
    }
    
    /**
     * Subscribe to this Flux with callbacks
     * 
     * @param onNext callback for each element
     */
    public void subscribe(Consumer<T> onNext) {
        subscribe(onNext, null, null);
    }
    
    /**
     * Subscribe to this Flux with callbacks
     * 
     * @param onNext callback for each element
     * @param onError callback for errors
     * @param onComplete callback when complete
     */
    public void subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        if (isStreaming && sink != null) {
            sink.setOnNext(onNext);
            sink.setOnError(onError);
            sink.setOnComplete(onComplete);
        } else {
            // Static mode - emit all elements immediately
            try {
                elements.forEach(onNext);
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        }
    }
    
    public List<T> collectList() {
        return new ArrayList<>(elements);
    }
    
    public Stream<T> toStream() {
        return elements.stream();
    }
    
    /**
     * FluxSink - Interface for emitting elements to a Flux
     */
    public static class FluxSink<T> {
        private final List<T> buffer = new CopyOnWriteArrayList<>();
        private Consumer<T> onNext;
        private Consumer<Throwable> onError;
        private Runnable onComplete;
        private boolean completed = false;
        private Throwable error;
        
        /**
         * Emit an element
         */
        public void next(T element) {
            if (completed || error != null) {
                throw new IllegalStateException("Cannot emit after completion or error");
            }
            buffer.add(element);
            if (onNext != null) {
                try {
                    onNext.accept(element);
                } catch (Exception e) {
                    error(e);
                }
            }
        }
        
        /**
         * Complete the stream
         */
        public void complete() {
            if (completed || error != null) {
                return;
            }
            completed = true;
            if (onComplete != null) {
                onComplete.run();
            }
        }
        
        /**
         * Emit an error
         */
        public void error(Throwable throwable) {
            if (completed || error != null) {
                return;
            }
            error = throwable;
            if (onError != null) {
                onError.accept(throwable);
            }
        }
        
        void setOnNext(Consumer<T> onNext) {
            this.onNext = onNext;
            // Replay buffered elements
            buffer.forEach(onNext);
        }
        
        void setOnError(Consumer<Throwable> onError) {
            this.onError = onError;
            if (error != null) {
                onError.accept(error);
            }
        }
        
        void setOnComplete(Runnable onComplete) {
            this.onComplete = onComplete;
            if (completed) {
                onComplete.run();
            }
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public boolean hasError() {
            return error != null;
        }
    }
}
