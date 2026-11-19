package com.unigram.threading;

import com.unigram.model.Message;

import java.util.List;

/**
 * Consumer thread worker in the Producer-Consumer architecture.
 * Dequeues messages from the thread-safe bounded MessageQueue and processes them
 * into a thread-safe sink collection without holding the queue lock during processing.
 */
public class MessageConsumer implements Runnable {

    private final String consumerName;
    private final MessageQueue queue;
    private final List<Message> processedSink;
    private final long processingDelayMillis;
    private int consumedCount;

    public MessageConsumer(String consumerName, MessageQueue queue, List<Message> processedSink, long processingDelayMillis) {
        if (consumerName == null || consumerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Consumer name cannot be null or empty.");
        }
        if (queue == null) {
            throw new IllegalArgumentException("MessageQueue cannot be null.");
        }
        if (processedSink == null) {
            throw new IllegalArgumentException("Processed sink list cannot be null.");
        }
        this.consumerName = consumerName.trim();
        this.queue = queue;
        this.processedSink = processedSink;
        this.processingDelayMillis = processingDelayMillis;
        this.consumedCount = 0;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Dequeue is synchronized inside MessageQueue; the lock is NOT held outside dequeue()
                Message msg = this.queue.dequeue();

                // When queue is closed AND empty, dequeue returns null signaling termination
                if (msg == null) {
                    System.out.printf("[%s] Queue is closed and empty. Consumer terminating gracefully.%n", consumerName);
                    break;
                }

                // Process message OUTSIDE the queue lock
                if (processingDelayMillis > 0) {
                    Thread.sleep(processingDelayMillis);
                }

                // Add to thread-safe sink
                synchronized (processedSink) {
                    this.processedSink.add(msg);
                }
                this.consumedCount++;

                System.out.printf("[%s] Processed msg: %s from sender: %s (Total processed by %s: %d)%n",
                        consumerName, msg.getMessageId(), msg.getSenderId(), consumerName, consumedCount);
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Consumer interrupted while waiting on queue. Exiting.%n", consumerName);
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        System.out.printf("[%s] Consumer finished. Total messages processed: %d%n", consumerName, consumedCount);
    }

    public String getConsumerName() {
        return consumerName;
    }

    public int getConsumedCount() {
        return consumedCount;
    }
}
