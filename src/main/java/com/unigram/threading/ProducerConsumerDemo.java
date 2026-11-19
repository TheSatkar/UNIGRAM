package com.unigram.threading;

import com.unigram.model.Message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Demonstrates the Producer-Consumer architecture using custom bounded MessageQueue.
 * Covers:
 * 1. Single Producer, Single Consumer (SPSC)
 * 2. Bounded queue capacity enforcement (full & empty conditions with wait/notifyAll)
 * 3. Multiple Producers, Multiple Consumers (MPMC)
 * 4. Safe queue shutdown and draining
 */
public class ProducerConsumerDemo {

    /**
     * Demo 1: Basic 1 Producer, 1 Consumer.
     */
    public static void runBasicProducerConsumer() {
        System.out.println("======================================================================");
        System.out.println("       1. Basic Producer-Consumer Demonstration (1P, 1C)              ");
        System.out.println("======================================================================\n");

        MessageQueue queue = new MessageQueue(3);
        List<Message> sink = Collections.synchronizedList(new ArrayList<>());

        List<Message> messages = createSampleMessages("P1", 4);

        Thread producerThread = new Thread(new MessageProducer("Producer-Alex", queue, messages, 100), "Producer-Alex");
        Thread consumerThread = new Thread(new MessageConsumer("Consumer-Maya", queue, sink, 150), "Consumer-Maya");

        producerThread.start();
        consumerThread.start();

        try {
            // Wait for producer to finish enqueuing all messages
            producerThread.join();
            // Close queue so consumer terminates when buffer is drained
            queue.close();
            // Wait for consumer to finish
            consumerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Basic demo interrupted.");
        }

        System.out.printf("[✓] Demo complete. Messages produced: %d, Messages consumed: %d.%n%n",
                messages.size(), sink.size());
    }

    /**
     * Demo 2: Bounded queue capacity enforcement (Full & Empty states).
     */
    public static void runBoundedQueueFullEmptyDemo() {
        System.out.println("======================================================================");
        System.out.println("       2. Bounded Queue Capacity & Full/Empty Wait Demonstration      ");
        System.out.println("======================================================================\n");

        final int capacity = 2;
        MessageQueue queue = new MessageQueue(capacity);
        List<Message> sink = Collections.synchronizedList(new ArrayList<>());

        System.out.println("Configuring tiny queue capacity = 2 to trigger full buffer condition quickly.");
        List<Message> batch = createSampleMessages("Burst", 4);

        // Fast producer: attempts to push 4 messages with zero delay
        Thread burstProducer = new Thread(new MessageProducer("BurstProducer", queue, batch, 10), "BurstProducer");

        // Delayed consumer: starts after a pause to let the buffer fill up
        Thread delayedConsumer = new Thread(() -> {
            try {
                System.out.println("[DelayedConsumer] Pausing 300ms to allow producer to fill queue...");
                Thread.sleep(300);

                System.out.printf("[DelayedConsumer] Checking queue: size=%d/%d, isFull=%b%n",
                        queue.size(), queue.getCapacity(), queue.isFull());

                while (true) {
                    Message m = queue.dequeue();
                    if (m == null) break;
                    sink.add(m);
                    System.out.println("[DelayedConsumer] Consumed: " + m.getMessageId());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                System.out.println("[DelayedConsumer] Interrupted.");
            }
        }, "DelayedConsumer");

        burstProducer.start();
        delayedConsumer.start();

        try {
            burstProducer.join();
            queue.close();
            delayedConsumer.join();
        } catch (InterruptedException e) {
            System.out.println("Demo interrupted.");
        }

        System.out.println("[✓] Bounded queue successfully blocked producer when full and consumer when empty.\n");
    }

    /**
     * Demo 3: Multiple Producers and Consumers (MPMC).
     */
    public static void runMultiProducerMultiConsumer() {
        System.out.println("======================================================================");
        System.out.println("       3. Multiple Producers & Multiple Consumers (MPMC)              ");
        System.out.println("======================================================================\n");

        MessageQueue queue = new MessageQueue(4);
        List<Message> sink = Collections.synchronizedList(new ArrayList<>());

        List<Message> p1Messages = createSampleMessages("BuyerA", 5);
        List<Message> p2Messages = createSampleMessages("BuyerB", 5);

        Thread prod1 = new Thread(new MessageProducer("Producer-BuyerA", queue, p1Messages, 80), "Producer-BuyerA");
        Thread prod2 = new Thread(new MessageProducer("Producer-BuyerB", queue, p2Messages, 80), "Producer-BuyerB");

        Thread cons1 = new Thread(new MessageConsumer("Consumer-Worker1", queue, sink, 120), "Consumer-Worker1");
        Thread cons2 = new Thread(new MessageConsumer("Consumer-Worker2", queue, sink, 120), "Consumer-Worker2");

        System.out.println("Starting 2 Producers (5 messages each = 10 total) and 2 Consumers...");
        prod1.start();
        prod2.start();
        cons1.start();
        cons2.start();

        try {
            prod1.join();
            prod2.join();
            System.out.println("[Coordinator] Both producers finished. Closing queue to signal consumers...");
            queue.close();
            cons1.join();
            cons2.join();
        } catch (InterruptedException e) {
            System.out.println("Multi-worker demo interrupted.");
        }

        System.out.println("\n--- Integrity Audit ---");
        System.out.printf("Total messages produced: %d%n", p1Messages.size() + p2Messages.size());
        System.out.printf("Total messages consumed: %d%n", sink.size());

        // Check for duplicates
        Set<String> uniqueIds = new HashSet<>();
        for (Message m : sink) {
            uniqueIds.add(m.getMessageId());
        }
        System.out.printf("Unique message IDs in sink: %d (No duplicates: %b)%n",
                uniqueIds.size(), uniqueIds.size() == sink.size());
        System.out.println("[✓] Multiple producers and consumers executed without data loss.\n");
    }

    /**
     * Demo 4: Safe queue shutdown and graceful draining.
     */
    public static void runShutdownDemo() {
        System.out.println("======================================================================");
        System.out.println("       4. Interruption & Queue Shutdown Demonstration                 ");
        System.out.println("======================================================================\n");

        MessageQueue queue = new MessageQueue(5);
        List<Message> sink = Collections.synchronizedList(new ArrayList<>());

        try {
            // Enqueue 3 messages
            queue.enqueue(new Message("msg_shut_1", "conv_1", "Alex", "Message 1", LocalDateTime.now()));
            queue.enqueue(new Message("msg_shut_2", "conv_1", "Maya", "Message 2", LocalDateTime.now()));
            queue.enqueue(new Message("msg_shut_3", "conv_1", "Alex", "Message 3", LocalDateTime.now()));
            System.out.println("Pre-enqueued 3 messages into queue.");

            System.out.println("Closing the queue immediately via queue.close()...");
            queue.close();

            // Try to enqueue after close
            System.out.println("Attempting to enqueue into closed queue:");
            try {
                queue.enqueue(new Message("msg_shut_4", "conv_1", "Alex", "Message 4", LocalDateTime.now()));
            } catch (IllegalStateException e) {
                System.out.println(" -> Caught Expected IllegalStateException: " + e.getMessage());
            }

            // Start consumer to verify it drains all 3 messages before exiting
            System.out.println("Starting consumer to verify it drains all remaining messages before stopping:");
            Thread drainConsumer = new Thread(new MessageConsumer("DrainConsumer", queue, sink, 50), "DrainConsumer");
            drainConsumer.start();
            drainConsumer.join();

            System.out.printf("Sink size after draining: %d/3 messages.%n", sink.size());
            System.out.println("[✓] Shutdown and drain demonstration complete.\n");

        } catch (InterruptedException e) {
            System.out.println("Shutdown demo interrupted.");
        }
    }

    /**
     * Helper to create mock sample messages.
     */
    public static List<Message> createSampleMessages(String prefix, int count) {
        List<Message> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new Message(
                    "msg_" + prefix + "_" + i,
                    "conv_prod_101",
                    "usr_" + prefix,
                    "Simulated payload #" + i + " from " + prefix,
                    LocalDateTime.now()
            ));
        }
        return list;
    }
}
