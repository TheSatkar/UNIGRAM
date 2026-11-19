package com.unigram.threading;

import com.unigram.model.Message;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Thread-safe custom bounded message queue implementing the Producer-Consumer pattern.
 * 
 * Uses intrinsic monitor locking (synchronized) and guarded suspension:
 * - wait() and notifyAll() inside condition loops (while) to handle spurious wakeups
 * - Enforces fixed capacity: producers wait when full
 * - Enforces availability: consumers wait when empty
 * - Order: Strict First-In-First-Out (FIFO)
 * - Safe shutdown: closing wakes all waiting threads and allows consumers to drain remaining messages
 */
public class MessageQueue {

    private final int capacity;
    private final Queue<Message> queue;
    private boolean closed;

    /**
     * Constructs a bounded queue with fixed maximum capacity.
     *
     * @param capacity maximum number of messages queue can hold (must be > 0)
     */
    public MessageQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be strictly greater than 0.");
        }
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
        this.closed = false;
    }

    /**
     * Enqueues a message into the bounded queue.
     * If the queue is full, the calling thread blocks and waits until space becomes available.
     *
     * @param message Message entity to enqueue (must not be null)
     * @throws InterruptedException  if the waiting thread is interrupted
     * @throws IllegalStateException if the queue is closed or closed while waiting
     */
    public synchronized void enqueue(Message message) throws InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("Cannot enqueue null message.");
        }

        // Guarded suspension loop to prevent spurious wakeups
        while (this.queue.size() >= this.capacity) {
            if (this.closed) {
                throw new IllegalStateException("Queue was closed while waiting to enqueue.");
            }
            wait();
        }

        if (this.closed) {
            throw new IllegalStateException("Cannot enqueue message: MessageQueue is closed.");
        }

        this.queue.offer(message);

        // Notify waiting consumer threads (and potentially other producers) that state has changed
        notifyAll();
    }

    /**
     * Dequeues and retrieves the next message in FIFO order.
     * If the queue is empty:
     * - If the queue is NOT closed, the calling thread blocks and waits until a message is enqueued.
     * - If the queue IS closed, returns null immediately to signal consumer termination.
     *
     * @return the dequeued Message, or null if the queue is closed and drained
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public synchronized Message dequeue() throws InterruptedException {
        // Guarded suspension loop for empty queue condition
        while (this.queue.isEmpty()) {
            if (this.closed) {
                // Queue is closed and all items have been drained; signal consumer to stop
                return null;
            }
            wait();
        }

        Message message = this.queue.poll();

        // Notify waiting producer threads that buffer space has been freed
        notifyAll();

        return message;
    }

    /**
     * Closes the queue safely.
     * Disallows future enqueues and wakes up all waiting producers and consumers.
     * Remaining queued messages can still be dequeued by consumers until empty.
     */
    public synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            notifyAll(); // Awaken all threads waiting on empty or full queue condition
        }
    }

    public synchronized boolean isClosed() {
        return this.closed;
    }

    public synchronized boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public synchronized boolean isFull() {
        return this.queue.size() >= this.capacity;
    }

    public synchronized int size() {
        return this.queue.size();
    }

    public int getCapacity() {
        return this.capacity;
    }
}
