package com.unigram.threading;

import com.unigram.model.Message;

import java.util.List;

/**
 * Concurrent producer thread worker that enqueues messages into the MessageQueue.
 * Handles queue-full backpressure (waiting when queue capacity is reached),
 * safe interruption, and marks items as QUEUED in the persistence tracker.
 */
public class JdbcMessageProducer implements Runnable {

    private final String producerName;
    private final MessageQueue queue;
    private final List<Message> messagesToProduce;
    private final MessagePersistenceTracker tracker;
    private final long delayMillis;
    private int producedCount;

    public JdbcMessageProducer(String producerName,
                               MessageQueue queue,
                               List<Message> messagesToProduce,
                               MessagePersistenceTracker tracker,
                               long delayMillis) {
        if (producerName == null || producerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Producer name cannot be null or empty.");
        }
        if (queue == null) {
            throw new IllegalArgumentException("MessageQueue cannot be null.");
        }
        if (messagesToProduce == null) {
            throw new IllegalArgumentException("Messages list cannot be null.");
        }
        if (tracker == null) {
            throw new IllegalArgumentException("MessagePersistenceTracker cannot be null.");
        }
        this.producerName = producerName.trim();
        this.queue = queue;
        this.messagesToProduce = messagesToProduce;
        this.tracker = tracker;
        this.delayMillis = delayMillis;
        this.producedCount = 0;
    }

    @Override
    public void run() {
        try {
            for (Message msg : messagesToProduce) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.printf("[%s] Interrupted before enqueuing '%s'. Halting.%n",
                            producerName, msg.getMessageId());
                    return;
                }

                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }

                // Register QUEUED status before enqueuing
                this.tracker.recordQueued(msg);

                // Enqueue: if queue is full, thread blocks and waits (guarded suspension backpressure)
                this.queue.enqueue(msg);
                this.producedCount++;

                System.out.printf("[%s] [QUEUED] Enqueued msg: %s (Queue size: %d/%d)%n",
                        producerName, msg.getMessageId(), this.queue.size(), this.queue.getCapacity());
            }
            System.out.printf("[%s] Successfully enqueued all %d messages.%n", producerName, producedCount);
        } catch (InterruptedException e) {
            System.out.printf("[%s] Interrupted while waiting on full queue. Exiting.%n", producerName);
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (IllegalStateException e) {
            System.out.printf("[%s] Queue closed (%s). Production halted.%n", producerName, e.getMessage());
        }
    }

    public String getProducerName() {
        return producerName;
    }

    public int getProducedCount() {
        return producedCount;
    }
}
