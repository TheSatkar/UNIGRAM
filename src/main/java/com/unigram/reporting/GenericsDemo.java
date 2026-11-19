package com.unigram.reporting;

import com.unigram.model.Message;
import com.unigram.model.User;
import com.unigram.util.ResultWrapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates Core Java Generics:
 * 1. Generic classes with type parameter <T>
 * 2. Multiple type parameters <K, V> (Pair)
 * 3. Bounded type parameters (<U extends Comparable<U>>)
 * 4. Wildcards: Upper-bounded (<? extends T>), Lower-bounded (<? super T>), Unbounded (<?>)
 * 5. PECS principle: Producer Extends, Consumer Super
 * 6. Compile-time type safety preventing ClassCastException
 */
public class GenericsDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     6. Java Generics & Wildcard Variance (PECS) Demonstration        ");
        System.out.println("======================================================================\n");

        // 1. Generic Class ResultWrapper<T>
        System.out.println("1. Generic Class ResultWrapper<T>:");
        User student = new User("usr_gen_1", "Donald Knuth", "knuth@stanford.edu");
        ResultWrapper<User> successResult = ResultWrapper.success(student);
        ResultWrapper<User> failureResult = ResultWrapper.failure("Student record missing from database.");

        System.out.printf("   Success wrapper isSuccess: %b | Data: %s%n",
                successResult.isSuccess(), successResult.getData().getFullName());
        System.out.printf("   Failure wrapper isSuccess: %b | Error: %s%n",
                failureResult.isSuccess(), failureResult.getErrorMessage());

        // Functor map over ResultWrapper
        ResultWrapper<String> emailWrapper = successResult.map(User::getEmail);
        System.out.println("   Mapped ResultWrapper<User> -> ResultWrapper<String>: " + emailWrapper.getData());

        // 2. Multiple Type Parameters: Pair<K, V>
        System.out.println("\n2. Multiple Type Parameters (Pair<K, V>):");
        ResultWrapper.Pair<String, Integer> studentAge = new ResultWrapper.Pair<>("Donald Knuth", 86);
        ResultWrapper.Pair<String, Message> latestMessagePair = new ResultWrapper.Pair<>(
                "conv_101",
                new Message("m1", "conv_101", "usr_gen_1", "The Art of Computer Programming", LocalDateTime.now())
        );

        System.out.println("   Student Age Pair       : " + studentAge);
        System.out.println("   Conversation Msg Pair  : Key=" + latestMessagePair.getKey() + " | Content=\"" + latestMessagePair.getValue().getContent() + "\"");

        // 3. Bounded Type Parameter: <U extends Comparable<U>>
        System.out.println("\n3. Bounded Type Parameter (<U extends Comparable<U>>):");
        String maxString = ResultWrapper.findMax("Algorithms", "DataStructures");
        Integer maxInt = ResultWrapper.findMax(42, 108);
        System.out.println("   findMax(\"Algorithms\", \"DataStructures\") -> " + maxString);
        System.out.println("   findMax(42, 108)                           -> " + maxInt);

        // 4. Wildcards & The PECS Principle (Producer Extends, Consumer Super)
        System.out.println("\n4. Wildcard Variance & The PECS Principle:");

        // Producer Extends: <? extends Number> producing numbers into List<Number>
        List<Integer> integers = List.of(10, 20, 30);
        List<Double> doubles = List.of(1.5, 2.5, 3.5);

        List<Number> numberSink = ResultWrapper.copyFromProducer(integers);
        numberSink.addAll(ResultWrapper.copyFromProducer(doubles));
        System.out.println("   Upper-Bounded (copyFromProducer <? extends Number>): " + numberSink);

        // Consumer Super: <? super Integer> consuming Integers
        List<Number> destinationNumbers = new ArrayList<>();
        List<Object> destinationObjects = new ArrayList<>();

        ResultWrapper.drainToConsumer(List.of(1, 2, 3), destinationNumbers);
        ResultWrapper.drainToConsumer(List.of(4, 5, 6), destinationObjects);
        System.out.println("   Lower-Bounded (drainToConsumer <? super Integer>) into List<Number>: " + destinationNumbers);
        System.out.println("   Lower-Bounded (drainToConsumer <? super Integer>) into List<Object>: " + destinationObjects);

        // Unbounded Wildcard: List<?>
        int totalElements = ResultWrapper.countElements(destinationObjects);
        System.out.println("   Unbounded Wildcard (countElements List<?>): " + totalElements);

        System.out.println("\n>> Academic Viva Note: Generics & Type Erasure <<");
        System.out.println("1. Compile-Time Type Safety: Generics prevent ClassCastException by enforcing types");
        System.out.println("   during compilation.");
        System.out.println("2. Type Erasure: The JVM removes generic type parameters during compilation for backward");
        System.out.println("   compatibility. At runtime, raw types (Object or bounded type) and synthetic casts are used.");
        System.out.println("3. PECS Guideline: Use 'extends' when reading data FROM a collection (Producer);");
        System.out.println("   use 'super' when writing data INTO a collection (Consumer).");
        System.out.println("======================================================================\n");
    }
}
