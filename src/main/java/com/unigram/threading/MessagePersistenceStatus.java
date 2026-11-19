package com.unigram.threading;

/**
 * State of a message within the concurrent persistence pipeline.
 */
public enum MessagePersistenceStatus {
    /**
     * Message is currently waiting in the bounded queue.
     */
    QUEUED,

    /**
     * Database transaction committed successfully to MySQL.
     */
    PERSISTED_SUCCESS,

    /**
     * Database operation failed (e.g. constraint violation or connection error); rolled back.
     */
    PERSISTENCE_FAILED
}
