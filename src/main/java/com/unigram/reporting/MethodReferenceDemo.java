package com.unigram.reporting;

import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.util.MessageFormatter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Demonstrates the 4 kinds of Method References in Java SE:
 * 1. Reference to a static method: ClassName::staticMethod
 * 2. Reference to an instance method of an arbitrary object: ClassName::instanceMethod
 * 3. Reference to an instance method of a particular object: instance::instanceMethod
 * 4. Reference to a constructor: ClassName::new
 */
public class MethodReferenceDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     3. Method References (4 Varieties) Demonstration                 ");
        System.out.println("======================================================================\n");

        Message sample = new Message("msg_ref_1", "conv_ref", "usr_prof", "Please review chapter 4.", LocalDateTime.now());

        // Variety 1: Static Method Reference (ClassName::staticMethod)
        System.out.println("1. Reference to a Static Method:");
        // Lambda: m -> MessageFormatter.formatConsole(m)
        // Method Reference: MessageFormatter::formatConsole
        Function<Message, String> staticRef = MessageFormatter::formatConsole;
        System.out.println("   Output via MessageFormatter::formatConsole:");
        System.out.println("   " + staticRef.apply(sample));

        // Variety 2: Instance Method of an Arbitrary Object of a Type (ClassName::instanceMethod)
        System.out.println("\n2. Reference to an Instance Method of an Arbitrary Object:");
        // Lambda: m -> m.getContent()
        // Method Reference: Message::getContent
        Function<Message, String> arbitraryInstanceRef = Message::getContent;
        System.out.println("   Extracted content via Message::getContent: \"" + arbitraryInstanceRef.apply(sample) + "\"");

        // Another example: String::toUpperCase
        Function<String, String> uppercaseRef = String::toUpperCase;
        System.out.println("   Uppercase via String::toUpperCase: \"" + uppercaseRef.apply(sample.getContent()) + "\"");

        // Variety 3: Instance Method of a Particular Existing Object (instance::instanceMethod)
        System.out.println("\n3. Reference to an Instance Method of a Particular Object:");
        // Lambda: s -> System.out.println(s)
        // Method Reference: System.out::println
        Consumer<String> sysOutRef = System.out::println;
        sysOutRef.accept("   Dispatched to console via System.out::println method reference.");

        // Variety 4: Constructor Reference (ClassName::new)
        System.out.println("\n4. Constructor Reference (ClassName::new):");
        // No-arg Constructor: Supplier<List<String>> listSupplier = ArrayList::new;
        Supplier<List<String>> listSupplier = ArrayList::new;
        List<String> freshlyCreatedList = listSupplier.get();
        freshlyCreatedList.add("Listing Alpha");
        freshlyCreatedList.add("Listing Beta");
        System.out.println("   Created collection via ArrayList::new (size: " + freshlyCreatedList.size() + ")");

        // Two-arg Constructor: BiFunction<String, String, ProductReference>
        BiFunction<String, String, ProductReference> productFactory = ProductReference::new;
        ProductReference prod = productFactory.apply("prod_ref_math", "Calculus with Analytical Geometry");
        System.out.printf("   Constructed ProductReference via ProductReference::new: [%s] \"%s\"%n",
                prod.getProductId(), prod.getProductTitle());

        System.out.println("\n>> Academic Viva Note: Method Reference Mechanics <<");
        System.out.println("Method references are syntactic shortcuts for lambda expressions that simply");
        System.out.println("forward arguments directly to an existing named method or constructor.");
        System.out.println("They improve readability by making intent immediately obvious.");
        System.out.println("======================================================================\n");
    }
}
