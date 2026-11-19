package com.unigram.threading;

import com.unigram.database.DatabaseConfig;
import com.unigram.model.Message;
import com.unigram.repository.MessageJdbcDao;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level coordinator managing concurrent message processing backed by MySQL.
 * Orchestrates a bounded MessageQueue, multiple JdbcMessageConsumer threads,
 * and a MessagePersistenceTracker.
 */
public class ConcurrentJdbcMessageEngine {

    private final MessageQueue queue;
    private final DatabaseConfig config;
    private final MessageJdbcDao messageDao;
    private final MessagePersistenceTracker tracker;
    private final List<Thread> consumerThreads;
    private final List<JdbcMessageConsumer> consumers;
    private boolean started;

    public ConcurrentJdbcMessageEngine(DatabaseConfig config, int queueCapacity, int numConsumers) {
        this(config, queueCapacity, numConsumers, 0);
    }

    public ConcurrentJdbcMessageEngine(DatabaseConfig config, int queueCapacity, int numConsumers, long consumerDelayMillis) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be > 0.");
        }
        if (numConsumers <= 0) {
            throw new IllegalArgumentException("Number of consumers must be > 0.");
        }

        this.config = config;
        this.queue = new MessageQueue(queueCapacity);
        this.messageDao = new MessageJdbcDao();
        this.tracker = new MessagePersistenceTracker();
        this.consumerThreads = new ArrayList<>(numConsumers);
        this.consumers = new ArrayList<>(numConsumers);
        this.started = false;

        for (int i = 1; i <= numConsumers; i++) {
            JdbcMessageConsumer consumer = new JdbcMessageConsumer(
                    "DbConsumer-" + i,
                    this.queue,
                    this.config,
                    this.messageDao,
                    this.tracker,
                    consumerDelayMillis
            );
            this.consumers.add(consumer);
            Thread t = new Thread(consumer, consumer.getConsumerName());
            this.consumerThreads.add(t);
        }
    }

    /**
     * Starts the consumer worker threads.
     */
    public synchronized void start() {
        if (!started) {
            for (Thread t : consumerThreads) {
                t.start();
            }
            this.started = true;
        }
    }

    /**
     * Enqueues a single message into the bounded queue and registers it as QUEUED.
     * Blocks if the bounded queue is currently full (backpressure).
     *
     * @param message Message to enqueue
     * @throws InterruptedException if thread is interrupted while blocked
     */
    public void enqueue(Message message) throws InterruptedException {
        this.tracker.recordQueued(message);
        this.queue.enqueue(message);
    }

    /**
     * Closes the queue safely, allows consumers to drain all remaining items into MySQL,
     * and waits for all consumer threads to terminate within the given timeout.
     *
     * @param timeoutMillis maximum milliseconds to wait for drain completion
     * @return true if all consumers terminated within timeout, false otherwise
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean shutdownAndAwait(long timeoutMillis) throws InterruptedException {
        // 1. Close queue to disallow further enqueues and signal consumers when empty
        this.queue.close();

        // 2. Wait for each consumer thread to finish draining
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (Thread t : consumerThreads) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining > 0) {
                t.join(remaining);
            } else {
                t.join(100);
            }
            if (t.isAlive()) {
                t.interrupt(); // Force interruption if timeout exceeded
            }
        }

        for (Thread t : consumerThreads) {
            if (t.isAlive()) {
                return false;
            }
        }
        return true;
    }

    public MessagePersistenceTracker getTracker() {
        return tracker;
    }

    public MessageQueue getQueue() {
        return queue;
    }

    public List<JdbcMessageConsumer> getConsumers() {
        return consumers;
    }
}
