package com.unigram.threading;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.model.Message;
import com.unigram.repository.MessageJdbcDao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Concurrent worker thread that dequeues messages from the custom bounded MessageQueue
 * and persists them into the MySQL database using isolated ACID transactions.
 *
 * Concurrency Safety Guarantees:
 * 1. The queue monitor lock is NEVER held during database I/O.
 * 2. Each consumer uses its own dedicated Connection (no shared Connection across threads).
 * 3. A message is only recorded as PERSISTED_SUCCESS after conn.commit() succeeds.
 * 4. Failed persistence triggers conn.rollback() and records PERSISTENCE_FAILED.
 * 5. Drains remaining queued messages before terminating gracefully on queue close.
 */
public class JdbcMessageConsumer implements Runnable {

    private final String consumerName;
    private final MessageQueue queue;
    private final DatabaseConfig config;
    private final MessageJdbcDao messageDao;
    private final MessagePersistenceTracker tracker;
    private final long processingDelayMillis;
    private int processedCount;
    private int failureCount;

    public JdbcMessageConsumer(String consumerName,
                               MessageQueue queue,
                               DatabaseConfig config,
                               MessageJdbcDao messageDao,
                               MessagePersistenceTracker tracker,
                               long processingDelayMillis) {
        if (consumerName == null || consumerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Consumer name cannot be null or empty.");
        }
        if (queue == null) {
            throw new IllegalArgumentException("MessageQueue cannot be null.");
        }
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        if (tracker == null) {
            throw new IllegalArgumentException("MessagePersistenceTracker cannot be null.");
        }
        this.consumerName = consumerName.trim();
        this.queue = queue;
        this.config = config;
        this.messageDao = messageDao != null ? messageDao : new MessageJdbcDao();
        this.tracker = tracker;
        this.processingDelayMillis = processingDelayMillis;
        this.processedCount = 0;
        this.failureCount = 0;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 1. Dequeue is synchronized inside MessageQueue; the queue lock is RELEASED upon return!
                Message msg = this.queue.dequeue();

                // When queue is closed AND empty, dequeue() returns null signaling graceful exit
                if (msg == null) {
                    System.out.printf("[%s] Queue closed and empty. Consumer terminating gracefully.%n", consumerName);
                    break;
                }

                // 2. Perform slow I/O or simulation delay OUTSIDE the queue monitor lock
                if (processingDelayMillis > 0) {
                    Thread.sleep(processingDelayMillis);
                }

                // 3. Open a dedicated database connection for this thread
                boolean committed = false;
                String failureReason = null;

                try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                    conn.setAutoCommit(false);
                    try {
                        messageDao.insert(conn, msg);
                        conn.commit();
                        committed = true;
                    } catch (SQLException e) {
                        conn.rollback();
                        failureReason = "SQL Error (State " + e.getSQLState() + "): " + e.getMessage();
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (Exception e) {
                    failureReason = "Connection Failure: " + e.getMessage();
                }

                // 4. Update persistence tracker based on atomic transaction outcome
                if (committed) {
                    this.processedCount++;
                    this.tracker.recordSuccess(consumerName, msg);
                    System.out.printf("[%s] [PERSISTED] Message '%s' committed to MySQL.%n",
                            consumerName, msg.getMessageId());
                } else {
                    this.failureCount++;
                    this.tracker.recordFailure(consumerName, msg, failureReason);
                    System.err.printf("[%s] [FAILED] Message '%s' rollback triggered: %s%n",
                            consumerName, msg.getMessageId(), failureReason);
                }
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Consumer interrupted while waiting on queue. Halting.%n", consumerName);
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        System.out.printf("[%s] Consumer finished. Successfully persisted: %d | Failed: %d%n",
                consumerName, processedCount, failureCount);
    }

    public String getConsumerName() {
        return consumerName;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getFailureCount() {
        return failureCount;
    }
}
