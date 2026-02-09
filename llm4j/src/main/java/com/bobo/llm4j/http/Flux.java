package com.bobo.llm4j.http;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Simple reactive stream implementation (simplified version of Reactor Flux)
 */
public class Flux<T> {
    
    private final List<T> elements;
    
    private Flux(List<T> elements) {
        this.elements = elements;
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
    
    public void subscribe(Consumer<T> consumer) {
        elements.forEach(consumer);
    }
    
    public List<T> collectList() {
        return new ArrayList<>(elements);
    }
    
    public Stream<T> toStream() {
        return elements.stream();
    }
}
