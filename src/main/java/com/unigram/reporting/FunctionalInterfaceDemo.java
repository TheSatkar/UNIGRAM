package com.unigram.reporting;

import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.User;
import com.unigram.util.MessageFilter;
import com.unigram.util.MessageFormatter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Demonstrates Functional Interfaces from java.util.function and custom SAM interfaces:
 * 1. Single Abstract Method (SAM) concept
 * 2. @FunctionalInterface annotation
 * 3. Built-in: Predicate<T>, Function<T, R>, Consumer<T>, Supplier<T>
 * 4. Custom interfaces: MessageFilter, MessageFormatter
 * 5. Passing behavior as method arguments (Higher-Order Functions)
 */
public class FunctionalInterfaceDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     1. Functional Interfaces & Single Abstract Method (SAM) Demo     ");
        System.out.println("======================================================================\n");

        // Sample test entities
        User studentA = new User("usr_demo_1", "Alice Smith", "alice@university.edu");
        Message msg1 = new Message("msg_1", "conv_101", "usr_demo_1", "Hi! Is the Biology book available?", LocalDateTime.now().minusMinutes(15));
        Message msg2 = new Message("msg_2", "conv_101", "usr_demo_2", "Yes, in perfect condition!", LocalDateTime.now().minusMinutes(5));
        Message msg3 = new Message("msg_3", "conv_101", "usr_demo_1", "Can I pick it up tomorrow for $30?", LocalDateTime.now().minusMinutes(2));

        List<Message> messageList = List.of(msg1, msg2, msg3);

        // 1. Built-in Predicate<T>: test(T t) -> boolean
        System.out.println("1. Built-in Predicate<T> (Message sender test):");
        Predicate<Message> isFromAlice = m -> "usr_demo_1".equals(m.getSenderId());
        for (Message m : messageList) {
            System.out.printf("   Message %s from Alice? %b%n", m.getMessageId(), isFromAlice.test(m));
        }

        // 2. Built-in Function<T, R>: apply(T t) -> R
        System.out.println("\n2. Built-in Function<T, R> (Message content extraction):");
        Function<Message, Integer> messageLength = m -> m.getContent().length();
        for (Message m : messageList) {
            System.out.printf("   Message %s length: %d characters%n", m.getMessageId(), messageLength.apply(m));
        }

        // 3. Built-in Consumer<T>: accept(T t) -> void
        System.out.println("\n3. Built-in Consumer<T> (Logging message notification):");
        Consumer<Message> notificationLogger = m ->
                System.out.printf("   [NOTIFICATION PUSH] User %s sent: \"%s\"%n", m.getSenderId(), m.getContent());
        messageList.forEach(notificationLogger);

        // 4. Built-in Supplier<T>: get() -> T
        System.out.println("\n4. Built-in Supplier<T> (Lazy generation of default message):");
        Supplier<Message> defaultMessageSupplier = () ->
                new Message("msg_def", "conv_101", "sys_bot", "Automated: Seller is currently away.", LocalDateTime.now());
        Message defaultMsg = defaultMessageSupplier.get();
        System.out.printf("   Generated default message ID: %s | Content: \"%s\"%n",
                defaultMsg.getMessageId(), defaultMsg.getContent());

        // 5. Custom Functional Interface: MessageFilter & Default Methods (and, or, negate)
        System.out.println("\n5. Custom SAM Interface (MessageFilter) with Composition (and, or, negate):");
        MessageFilter sentByAlice = m -> "usr_demo_1".equals(m.getSenderId());
        MessageFilter containsPrice = m -> m.getContent().contains("$");

        MessageFilter aliceAskingPrice = sentByAlice.and(containsPrice);
        for (Message m : messageList) {
            System.out.printf("   Message %s: Sent by Alice AND mentions price? %b%n",
                    m.getMessageId(), aliceAskingPrice.accept(m));
        }

        // 6. Passing Behavior as Parameter (Higher-Order Function)
        System.out.println("\n6. Passing Behavior into Higher-Order Method:");
        List<Message> filtered = applyFilter(messageList, containsPrice);
        System.out.printf("   Messages matching price filter: %d found.%n", filtered.size());
        for (Message m : filtered) {
            System.out.printf("     - [%s] \"%s\"%n", m.getMessageId(), m.getContent());
        }

        System.out.println("\n>> Academic Viva Note: Single Abstract Method (SAM) <<");
        System.out.println("A functional interface has EXACTLY one abstract method. It may also define");
        System.out.println("any number of default or static methods. The @FunctionalInterface annotation");
        System.out.println("enforces this constraint at compile time.");
        System.out.println("======================================================================\n");
    }

    /**
     * Higher-order method accepting a behavior parameter (MessageFilter).
     */
    public static List<Message> applyFilter(List<Message> messages, MessageFilter filter) {
        List<Message> result = new ArrayList<>();
        for (Message m : messages) {
            if (filter.accept(m)) {
                result.add(m);
            }
        }
        return result;
    }
}
