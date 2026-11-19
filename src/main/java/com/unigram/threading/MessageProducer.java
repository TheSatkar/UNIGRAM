package com.unigram.threading;

import com.unigram.model.Message;

import java.util.List;

/**
 * Producer thread worker in the Producer-Consumer architecture.
 * Enqueues messages into the thread-safe bounded MessageQueue.
 */
public class MessageProducer implements Runnable {

    private final String producerName;
    private final MessageQueue queue;
    private final List<Message> messagesToProduce;
    private final long delayMillis;
    private int producedCount;

    public MessageProducer(String producerName, MessageQueue queue, List<Message> messagesToProduce, long delayMillis) {
        if (producerName == null || producerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Producer name cannot be null or empty.");
        }
        if (queue == null) {
            throw new IllegalArgumentException("MessageQueue cannot be null.");
        }
        if (messagesToProduce == null) {
            throw new IllegalArgumentException("Messages list cannot be null.");
        }
        this.producerName = producerName.trim();
        this.queue = queue;
        this.messagesToProduce = messagesToProduce;
        this.delayMillis = delayMillis;
        this.producedCount = 0;
    }

    @Override
    public void run() {
        try {
            for (Message msg : messagesToProduce) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.printf("[%s] Interrupted before enqueuing message '%s'. Exiting.%n",
                            producerName, msg.getMessageId());
                    return;
                }

                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }

                this.queue.enqueue(msg);
                this.producedCount++;
                System.out.printf("[%s] Enqueued msg: %s (Queue size: %d/%d)%n",
                        producerName, msg.getMessageId(), this.queue.size(), this.queue.getCapacity());
            }
            System.out.printf("[%s] Successfully enqueued all %d messages.%n", producerName, producedCount);
        } catch (InterruptedException e) {
            System.out.printf("[%s] Interrupted while waiting on queue. Halting.%n", producerName);
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (IllegalStateException e) {
            System.out.printf("[%s] Queue was closed: %s. Halting production.%n", producerName, e.getMessage());
        }
    }

    public String getProducerName() {
        return producerName;
    }

    public int getProducedCount() {
        return producedCount;
    }
}
