package com.unigram.reporting;

import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comprehensive demonstration of the Java Stream API (java.util.stream):
 * 1. Creating streams from collections
 * 2. Intermediate operations: filter(), map(), sorted(), distinct(), limit()
 * 3. Terminal operations: forEach(), collect(), count(), reduce()
 * 4. Advanced Collectors: groupingBy(), partitioningBy(), joining()
 * 5. Sequential vs Parallel Streams (ForkJoinPool behavior & JDBC exclusion warning)
 * 6. Realistic Uni-Gram marketplace messaging analytics
 */
public class StreamApiDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     4. Stream API (Intermediate & Terminal Operations) Demo          ");
        System.out.println("======================================================================\n");

        // Seed realistic demonstration dataset
        User studentA = new User("usr_alex", "Alex Chen", "alex@uni.edu");
        User studentB = new User("usr_maya", "Maya Patel", "maya@uni.edu");
        User studentC = new User("usr_jordan", "Jordan Lee", "jordan@uni.edu");

        ProductReference bookAlgo = new ProductReference("prod_algo", "Introduction to Algorithms (4th Ed)");
        ProductReference calcTi84 = new ProductReference("prod_ti84", "TI-84 Plus Graphing Calculator");
        ProductReference labCoat = new ProductReference("prod_coat", "Organic Chemistry Lab Coat (Medium)");

        Conversation conv1 = new Conversation("conv_01", studentA.getUserId(), studentB.getUserId(), bookAlgo.getProductId());
        Conversation conv2 = new Conversation("conv_02", studentC.getUserId(), studentB.getUserId(), calcTi84.getProductId());
        Conversation conv3 = new Conversation("conv_03", studentA.getUserId(), studentC.getUserId(), labCoat.getProductId());

        List<Conversation> conversations = List.of(conv1, conv2, conv3);

        LocalDateTime now = LocalDateTime.now();
        List<Message> messages = List.of(
                new Message("m1", "conv_01", "usr_alex", "Hi Maya, is the CLRS algorithms book still for sale?", now.minusMinutes(45)),
                new Message("m2", "conv_01", "usr_maya", "Yes! $50 and it includes code notes.", now.minusMinutes(40)),
                new Message("m3", "conv_01", "usr_alex", "Would you take $45 cash today?", now.minusMinutes(35)),
                new Message("m4", "conv_01", "usr_maya", "Deal. Let's meet at the campus cafe.", now.minusMinutes(30)),
                new Message("m5", "conv_02", "usr_jordan", "Hey Maya, still selling the TI-84?", now.minusMinutes(25)),
                new Message("m6", "conv_02", "usr_maya", "Yes Jordan, batteries are fresh.", now.minusMinutes(20)),
                new Message("m7", "conv_03", "usr_alex", "Hi Jordan, what size is the lab coat?", now.minusMinutes(15)),
                new Message("m8", "conv_03", "usr_jordan", "It is unisex size Medium, washed and sanitized.", now.minusMinutes(10)),
                new Message("m9", "conv_03", "usr_alex", "Perfect, I will take it!", now.minusMinutes(5))
        );

        // 1. filter() & collect(Collectors.toList()): Find all messages sent by Alex
        System.out.println("1. filter() & collect(): Messages sent by Alex (usr_alex):");
        List<Message> alexMessages = messages.stream()
                .filter(m -> "usr_alex".equals(m.getSenderId()))
                .collect(Collectors.toList());
        alexMessages.forEach(m -> System.out.printf("   - [%s] \"%s\"%n", m.getMessageId(), m.getContent()));

        // 2. map() & distinct(): Extract all unique sender IDs across the entire conversation history
        System.out.println("\n2. map() & distinct(): Distinct student IDs actively messaging:");
        List<String> distinctSenders = messages.stream()
                .map(Message::getSenderId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println("   Active participants: " + distinctSenders);

        // 3. sorted() & limit(): Sort messages by content length descending and pick top 3 longest messages
        System.out.println("\n3. sorted() & limit(): Top 3 longest message contents:");
        List<Message> longestMessages = messages.stream()
                .sorted(Comparator.comparingInt((Message m) -> m.getContent().length()).reversed())
                .limit(3)
                .collect(Collectors.toList());
        longestMessages.forEach(m -> System.out.printf("   - (%d chars) [%s] \"%s\"%n",
                m.getContent().length(), m.getSenderId(), m.getContent()));

        // 4. count(): Messages containing deal / price discussions
        System.out.println("\n4. count(): Messages mentioning price or deal keywords:");
        long dealMentions = messages.stream()
                .filter(m -> m.getContent().contains("$") || m.getContent().toLowerCase().contains("deal"))
                .count();
        System.out.println("   Count of pricing/deal messages: " + dealMentions);

        // 5. reduce(): Total character count across all messages
        System.out.println("\n5. reduce(): Total cumulative character count across all messages:");
        int totalChars = messages.stream()
                .map(m -> m.getContent().length())
                .reduce(0, (acc, len) -> acc + len);
        System.out.println("   Total text characters transferred: " + totalChars);

        // 6. Collectors.groupingBy(): Group messages by conversation ID
        System.out.println("\n6. Collectors.groupingBy(): Messages grouped by Conversation ID:");
        Map<String, List<Message>> messagesByConv = messages.stream()
                .collect(Collectors.groupingBy(Message::getConversationId));
        messagesByConv.forEach((cid, msgList) ->
                System.out.printf("   Conversation %s: %d message(s)%n", cid, msgList.size()));

        // 7. Collectors.partitioningBy(): Partition messages into buyer messages vs seller messages for conv_01
        System.out.println("\n7. Collectors.partitioningBy(): Partition conv_01 messages (Alex vs Others):");
        Map<Boolean, List<Message>> partitioned = messages.stream()
                .filter(m -> "conv_01".equals(m.getConversationId()))
                .collect(Collectors.partitioningBy(m -> "usr_alex".equals(m.getSenderId())));
        System.out.printf("   Alex (Buyer) messages : %d%n", partitioned.get(true).size());
        System.out.printf("   Maya (Seller) messages: %d%n", partitioned.get(false).size());

        // 8. Collectors.joining(): Build a single joined summary transcript
        System.out.println("\n8. Collectors.joining(): Combined chat transcript for conv_01:");
        String transcript = messages.stream()
                .filter(m -> "conv_01".equals(m.getConversationId()))
                .map(m -> m.getSenderId() + ": " + m.getContent())
                .collect(Collectors.joining(" | "));
        System.out.println("   " + transcript);

        // 9. Finding most active conversation using max()
        System.out.println("\n9. Aggregation: Most active conversation:");
        Optional<Map.Entry<String, List<Message>>> mostActive = messagesByConv.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()));
        mostActive.ifPresent(entry ->
                System.out.printf("   Conversation %s has the highest volume (%d messages)%n",
                        entry.getKey(), entry.getValue().size()));

        // 10. Sequential vs Parallel Streams Demonstration
        System.out.println("\n10. Sequential vs Parallel Streams Demonstration:");
        System.out.println("   Sequential Stream Thread Execution:");
        messages.stream().limit(3).forEach(m ->
                System.out.printf("     [Sequential] Thread: '%s' processing %s%n",
                        Thread.currentThread().getName(), m.getMessageId()));

        System.out.println("   Parallel Stream (ForkJoinPool) Thread Execution:");
        messages.parallelStream().limit(4).forEach(m ->
                System.out.printf("     [Parallel]   Thread: '%s' processing %s%n",
                        Thread.currentThread().getName(), m.getMessageId()));

        System.out.println("\n>> Academic Viva Note: Streams & Concurrency Rules <<");
        System.out.println("1. Non-Interference: Stream pipelines operate on elements without mutating the source.");
        System.out.println("2. Lazy Evaluation: Intermediate operations are not executed until a terminal operation");
        System.out.println("   is invoked (e.g. collect, count, forEach).");
        System.out.println("3. JDBC Rule: Parallel streams MUST NOT be used with ordinary JDBC operations because");
        System.out.println("   JDBC Connections are NOT thread-safe and ForkJoinPool threads share state unpredictably.");
        System.out.println("======================================================================\n");
    }
}
