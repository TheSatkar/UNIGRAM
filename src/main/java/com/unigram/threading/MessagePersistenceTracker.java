package com.unigram.threading;

import com.unigram.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe tracker recording the lifecycle status of messages in the concurrent pipeline.
 *
 * Guarantees that:
 * 1. A message is never reported as PERSISTED_SUCCESS until its MySQL transaction commits.
 * 2. Failed persistence is explicitly recorded as PERSISTENCE_FAILED with error details.
 * 3. Enqueued messages waiting in the bounded queue are marked QUEUED.
 */
public class MessagePersistenceTracker {

    public static class Record {
        private final Message message;
        private final MessagePersistenceStatus status;
        private final String workerName;
        private final String errorMessage;
        private final long timestamp;

        public Record(Message message, MessagePersistenceStatus status, String workerName, String errorMessage) {
            this.message = message;
            this.status = status;
            this.workerName = workerName;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }

        public Message getMessage() {
            return message;
        }

        public MessagePersistenceStatus getStatus() {
            return status;
        }

        public String getWorkerName() {
            return workerName;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private final Map<String, Record> records = new HashMap<>();
    private final List<Message> persistedMessages = new ArrayList<>();
    private final List<Message> failedMessages = new ArrayList<>();

    public synchronized void recordQueued(Message message) {
        if (message == null) return;
        records.put(message.getMessageId(), new Record(message, MessagePersistenceStatus.QUEUED, "Producer", null));
    }

    public synchronized void recordSuccess(String workerName, Message message) {
        if (message == null) return;
        records.put(message.getMessageId(), new Record(message, MessagePersistenceStatus.PERSISTED_SUCCESS, workerName, null));
        persistedMessages.add(message);
    }

    public synchronized void recordFailure(String workerName, Message message, String errorMessage) {
        if (message == null) return;
        records.put(message.getMessageId(), new Record(message, MessagePersistenceStatus.PERSISTENCE_FAILED, workerName, errorMessage));
        failedMessages.add(message);
    }

    public synchronized MessagePersistenceStatus getStatus(String messageId) {
        Record r = records.get(messageId);
        return r != null ? r.getStatus() : null;
    }

    public synchronized Record getRecord(String messageId) {
        return records.get(messageId);
    }

    public synchronized List<Message> getPersistedMessages() {
        return Collections.unmodifiableList(new ArrayList<>(persistedMessages));
    }

    public synchronized List<Message> getFailedMessages() {
        return Collections.unmodifiableList(new ArrayList<>(failedMessages));
    }

    public synchronized int getSuccessCount() {
        return persistedMessages.size();
    }

    public synchronized int getFailureCount() {
        return failedMessages.size();
    }

    public synchronized int getTotalTrackedCount() {
        return records.size();
    }
}
