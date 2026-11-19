package com.unigram.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Pedagogical generic container demonstrating core Java Generics concepts:
 * 1. Generic class with type parameter <T>
 * 2. Static generic factory methods
 * 3. Type safety avoiding runtime ClassCastException
 * 4. Bounded type parameters (<U extends Comparable<U>>)
 * 5. Upper-bounded wildcards (<? extends T> - PECS Producer Extends)
 * 6. Lower-bounded wildcards (<? super T> - PECS Consumer Super)
 * 7. Unbounded wildcards (<?>)
 */
public class ResultWrapper<T> {

    private final T data;
    private final String errorMessage;
    private final boolean success;

    private ResultWrapper(T data, String errorMessage, boolean success) {
        this.data = data;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    /**
     * Generic static factory method producing a successful ResultWrapper.
     */
    public static <T> ResultWrapper<T> success(T data) {
        return new ResultWrapper<>(data, null, true);
    }

    /**
     * Generic static factory method producing a failed ResultWrapper.
     */
    public static <T> ResultWrapper<T> failure(String errorMessage) {
        return new ResultWrapper<>(null, errorMessage, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        if (!success) {
            throw new IllegalStateException("Cannot retrieve data from failed result: " + errorMessage);
        }
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Transforms the enclosed value using a mapping function (Functor map).
     */
    public <R> ResultWrapper<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null.");
        if (!success) {
            return ResultWrapper.failure(errorMessage);
        }
        return ResultWrapper.success(mapper.apply(data));
    }

    // -------------------------------------------------------------------------
    // Bounded Generic Methods & PECS Demonstrations
    // -------------------------------------------------------------------------

    /**
     * Generic method with bounded type parameter <U extends Comparable<U>>.
     * Compares two elements and returns the greater one.
     */
    public static <U extends Comparable<U>> U findMax(U first, U second) {
        Objects.requireNonNull(first, "First argument cannot be null.");
        Objects.requireNonNull(second, "Second argument cannot be null.");
        return first.compareTo(second) >= 0 ? first : second;
    }

    /**
     * Demonstrates Upper Bounded Wildcard (<? extends T>).
     * PECS rule: Producer Extends. The source list produces items of type T.
     */
    public static <T> List<T> copyFromProducer(List<? extends T> producerList) {
        List<T> destination = new ArrayList<>();
        for (T item : producerList) {
            destination.add(item);
        }
        return destination;
    }

    /**
     * Demonstrates Lower Bounded Wildcard (<? super T>).
     * PECS rule: Consumer Super. The destination list consumes items of type T.
     */
    public static <T> void drainToConsumer(List<T> source, List<? super T> consumerList) {
        for (T item : source) {
            consumerList.add(item);
        }
    }

    /**
     * Demonstrates Unbounded Wildcard (<?>).
     * Operates on a collection of unknown element type.
     */
    public static int countElements(List<?> list) {
        return list != null ? list.size() : 0;
    }

    /**
     * Generic class with multiple type parameters (Pair<K, V>).
     */
    public static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "(" + key + " -> " + value + ")";
        }
    }
}
