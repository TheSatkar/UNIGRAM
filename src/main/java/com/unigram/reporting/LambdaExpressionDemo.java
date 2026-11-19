package com.unigram.reporting;

import com.unigram.model.Message;
import com.unigram.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Demonstrates Lambda Expressions in Java SE:
 * 1. Syntax variants: zero, single, and multiple parameters
 * 2. Expression lambdas vs Block lambdas
 * 3. Comparison with Anonymous Inner Classes (Bytecode, this pointer, lexical scoping)
 * 4. Variable capture rules (effectively final requirement)
 */
public class LambdaExpressionDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     2. Lambda Expressions & Anonymous Class Comparison               ");
        System.out.println("======================================================================\n");

        User student1 = new User("usr_stu_1", "Brian Kernighan", "brian@bell-labs.com");
        User student2 = new User("usr_stu_2", "Ken Thompson", "ken@bell-labs.com");

        Message msgA = new Message("m1", "c1", "usr_stu_1", "Hello world!", LocalDateTime.now());
        Message msgB = new Message("m2", "c1", "usr_stu_2", "Unix rules.", LocalDateTime.now().plusSeconds(10));

        // 1. Zero Parameters: () -> ...
        System.out.println("1. Zero-Parameter Lambda:");
        Supplier<String> bannerSupplier = () -> "=== Uni-Gram Notification Dispatcher ===";
        System.out.println("   " + bannerSupplier.get());

        // 2. Single Parameter (parentheses optional): x -> ...
        System.out.println("\n2. Single-Parameter Expression Lambda:");
        Consumer<String> logger = msg -> System.out.println("   [LOG] " + msg);
        logger.accept("Dispatched message ID: m1");

        // 3. Multiple Parameters: (a, b) -> ...
        System.out.println("\n3. Multi-Parameter Lambda:");
        BiPredicate<User, String> emailMatchesDomain = (u, domain) -> u.getEmail().endsWith(domain);
        System.out.printf("   Is Brian's email on bell-labs.com? %b%n", emailMatchesDomain.test(student1, "@bell-labs.com"));
        System.out.printf("   Is Brian's email on university.edu? %b%n", emailMatchesDomain.test(student1, "@university.edu"));

        // 4. Expression Lambda vs Block Lambda
        System.out.println("\n4. Expression Lambda vs Block Lambda Comparison:");

        // Expression Lambda (single expression, implicit return)
        Comparator<Message> lengthComparatorExpr = (m1, m2) -> Integer.compare(m1.getContent().length(), m2.getContent().length());

        // Block Lambda (explicit braces, multiple statements, explicit return)
        Comparator<Message> lengthComparatorBlock = (m1, m2) -> {
            int len1 = m1.getContent().length();
            int len2 = m2.getContent().length();
            System.out.printf("   [Block Lambda Evaluation] Comparing '%s' (len %d) vs '%s' (len %d)%n",
                    m1.getMessageId(), len1, m2.getMessageId(), len2);
            return Integer.compare(len1, len2);
        };

        int compResult = lengthComparatorBlock.compare(msgA, msgB);
        System.out.printf("   Comparison result: %d%n", compResult);

        // 5. Lambda vs Anonymous Inner Class Comparison
        System.out.println("\n5. Lambda vs Anonymous Inner Class Comparison:");

        // Approach A: Anonymous Inner Class
        Comparator<Message> anonymousComparator = new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o1.getSentAt().compareTo(o2.getSentAt());
            }
        };

        // Approach B: Lambda Equivalent
        Comparator<Message> lambdaComparator = (o1, o2) -> o1.getSentAt().compareTo(o2.getSentAt());

        System.out.println("   Anonymous Inner Class: " + anonymousComparator.getClass().getName());
        System.out.println("   Lambda Expression    : " + lambdaComparator.getClass().getName());

        // 6. Variable capture (Effectively Final rule)
        String prefix = "[UNI-MARKET] "; // Effectively final variable
        Consumer<Message> prefixedPrinter = m -> System.out.println("   " + prefix + m.getContent());
        prefixedPrinter.accept(msgA);

        System.out.println("\n>> Academic Viva Note: Lambdas vs Anonymous Inner Classes <<");
        System.out.println("1. Bytecode: Anonymous classes generate separate class files on disk (Enclosing$1.class).");
        System.out.println("   Lambdas use the 'invokedynamic' instruction and LambdaMetafactory at runtime.");
        System.out.println("2. Lexical Scoping: In an anonymous class, 'this' refers to the anonymous instance itself.");
        System.out.println("   In a lambda expression, 'this' refers to the enclosing outer class instance.");
        System.out.println("3. Memory: Lambdas avoid object instantiation overhead when no state is captured.");
        System.out.println("======================================================================\n");
    }
}
