package com.unigram;

import com.unigram.model.Message;
import com.unigram.threading.MessageConsumer;
import com.unigram.threading.MessageProducer;
import com.unigram.threading.MessageQueue;
import com.unigram.threading.SynchronizationRaceDemo.SafeCounter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Automated Verification Suite for Phase 4: Synchronization and Producer-Consumer Architecture.
 * 
 * Verifies:
 * 1. Strict FIFO queue ordering
 * 2. Queue capacity enforcement and producer blocking when full
 * 3. Consumer blocking when empty and wakeup upon enqueue
 * 4. Queue closure wakes waiting threads & disallows new enqueues
 * 5. Consumers drain remaining queued messages before exiting upon close
 * 6. Multi-producer multi-consumer execution without message loss or duplication
 * 7. Clean InterruptedException handling
 * 8. Thread-safe counter synchronization preventing race conditions
 * 9. Practical BLOCKED and WAITING thread state observation
 * 10. Clean shutdown ensuring no worker threads remain alive
 */
public class Phase4Verification {

    public static void main(String[] args) {
        System.out.println(">>> Running Phase 4 Synchronization & Producer-Consumer Verification Suite <<<\n");
        int testCasesPassed = 0;

        try {
            // Test 1: FIFO Ordering
            MessageQueue fifoQueue = new MessageQueue(5);
            Message m1 = createMsg("m1");
            Message m2 = createMsg("m2");
            Message m3 = createMsg("m3");

            fifoQueue.enqueue(m1);
            fifoQueue.enqueue(m2);
            fifoQueue.enqueue(m3);

            Message out1 = fifoQueue.dequeue();
            Message out2 = fifoQueue.dequeue();
            Message out3 = fifoQueue.dequeue();

            assertTrue(out1.getMessageId().equals("m1"), "First dequeued is m1 (FIFO)");
            assertTrue(out2.getMessageId().equals("m2"), "Second dequeued is m2 (FIFO)");
            assertTrue(out3.getMessageId().equals("m3"), "Third dequeued is m3 (FIFO)");
            testCasesPassed++;

            // Test 2: Queue Capacity Enforcement & Producer Blocking When Full
            MessageQueue boundedQueue = new MessageQueue(2);
            boundedQueue.enqueue(createMsg("b1"));
            boundedQueue.enqueue(createMsg("b2"));
            assertTrue(boundedQueue.isFull(), "Queue reports isFull() == true when capacity reached");

            AtomicBoolean producerEnteredWait = new AtomicBoolean(false);
            Thread fullProducer = new Thread(() -> {
                try {
                    producerEnteredWait.set(true);
                    boundedQueue.enqueue(createMsg("b3_blocking")); // Must block
                } catch (InterruptedException ignored) {}
            }, "FullProducerWorker");

            fullProducer.start();
            Thread.sleep(60); // Allow thread to run and enter wait()
            assertTrue(fullProducer.getState() == Thread.State.WAITING,
                    "Producer entered WAITING state when enqueueing to full queue");

            // Dequeue one item to create room
            Message freedMsg = boundedQueue.dequeue();
            assertTrue(freedMsg.getMessageId().equals("b1"), "Dequeued item freed space");

            fullProducer.join(500); // Producer should now unblock and complete
            assertTrue(!fullProducer.isAlive(), "Blocked producer awakened and completed successfully");
            assertTrue(boundedQueue.size() == 2, "Queue size remains valid after unblocked enqueue");
            testCasesPassed++;

            // Test 3: Consumer Blocking When Empty & Wakeup on Enqueue
            MessageQueue emptyQueue = new MessageQueue(2);
            AtomicReference<Message> consumedHolder = new AtomicReference<>();
            Thread emptyConsumer = new Thread(() -> {
                try {
                    Message msg = emptyQueue.dequeue(); // Must block
                    consumedHolder.set(msg);
                } catch (InterruptedException ignored) {}
            }, "EmptyConsumerWorker");

            emptyConsumer.start();
            Thread.sleep(60); // Allow thread to enter wait()
            assertTrue(emptyConsumer.getState() == Thread.State.WAITING,
                    "Consumer entered WAITING state when dequeuing from empty queue");

            // Enqueue an item to wake up consumer
            emptyQueue.enqueue(createMsg("wakeup_msg"));
            emptyConsumer.join(500);
            assertTrue(!emptyConsumer.isAlive(), "Blocked consumer awakened and terminated");
            assertTrue(consumedHolder.get() != null && consumedHolder.get().getMessageId().equals("wakeup_msg"),
                    "Consumer successfully received message after wakeup");
            testCasesPassed++;

            // Test 4: Queue Closure Wakes Waiting Threads & Rejects Future Enqueues
            MessageQueue closeTestQueue = new MessageQueue(3);
            AtomicBoolean consumerNotifiedOnClose = new AtomicBoolean(false);
            Thread waitingOnCloseConsumer = new Thread(() -> {
                try {
                    Message m = closeTestQueue.dequeue(); // Block until close
                    if (m == null) {
                        consumerNotifiedOnClose.set(true); // Closed & empty signal
                    }
                } catch (InterruptedException ignored) {}
            }, "WaitingOnCloseConsumer");

            waitingOnCloseConsumer.start();
            Thread.sleep(60);
            closeTestQueue.close();
            waitingOnCloseConsumer.join(500);
            assertTrue(consumerNotifiedOnClose.get(), "close() awakened waiting consumer returning null");

            boolean caughtEnqueueAfterClose = false;
            try {
                closeTestQueue.enqueue(createMsg("after_close"));
            } catch (IllegalStateException e) {
                caughtEnqueueAfterClose = true;
            }
            assertTrue(caughtEnqueueAfterClose, "enqueue() rejected with IllegalStateException after queue close");
            testCasesPassed++;

            // Test 5: Consumers Drain Remaining Messages Before Exiting
            MessageQueue drainQueue = new MessageQueue(5);
            drainQueue.enqueue(createMsg("drain_1"));
            drainQueue.enqueue(createMsg("drain_2"));
            drainQueue.enqueue(createMsg("drain_3"));
            drainQueue.close(); // Closed while 3 messages remain

            List<Message> drainedSink = Collections.synchronizedList(new ArrayList<>());
            Thread drainWorker = new Thread(new MessageConsumer("DrainWorker", drainQueue, drainedSink, 10), "DrainWorker");
            drainWorker.start();
            drainWorker.join(1000);

            assertTrue(!drainWorker.isAlive(), "Drain consumer terminated cleanly");
            assertTrue(drainedSink.size() == 3, "Consumer drained all 3 messages before stopping");
            assertTrue(drainQueue.isEmpty(), "Queue is empty after full drain");
            testCasesPassed++;

            // Test 6: Multi-Producer Multi-Consumer (MPMC) Data Integrity
            MessageQueue mpmcQueue = new MessageQueue(4);
            List<Message> mpmcSink = Collections.synchronizedList(new ArrayList<>());

            List<Message> batchA = createMsgBatch("prodA", 8);
            List<Message> batchB = createMsgBatch("prodB", 8);

            Thread pA = new Thread(new MessageProducer("PA", mpmcQueue, batchA, 5), "PA");
            Thread pB = new Thread(new MessageProducer("PB", mpmcQueue, batchB, 5), "PB");
            Thread cA = new Thread(new MessageConsumer("CA", mpmcQueue, mpmcSink, 10), "CA");
            Thread cB = new Thread(new MessageConsumer("CB", mpmcQueue, mpmcSink, 10), "CB");

            pA.start();
            pB.start();
            cA.start();
            cB.start();

            pA.join(2000);
            pB.join(2000);
            mpmcQueue.close();
            cA.join(2000);
            cB.join(2000);

            assertTrue(mpmcSink.size() == 16, "All 16 messages consumed in MPMC scenario");
            Set<String> uniqueIds = new HashSet<>();
            for (Message m : mpmcSink) uniqueIds.add(m.getMessageId());
            assertTrue(uniqueIds.size() == 16, "No messages lost or duplicated in MPMC scenario (16 unique IDs)");
            testCasesPassed++;

            // Test 7: InterruptedException Handling
            MessageQueue interruptQueue = new MessageQueue(2);
            AtomicBoolean interruptedCaught = new AtomicBoolean(false);
            Thread interruptedWorker = new Thread(() -> {
                try {
                    interruptQueue.dequeue();
                } catch (InterruptedException e) {
                    interruptedCaught.set(true);
                }
            }, "InterruptedWorker");

            interruptedWorker.start();
            Thread.sleep(60);
            interruptedWorker.interrupt();
            interruptedWorker.join(500);
            assertTrue(interruptedCaught.get(), "Thread correctly caught InterruptedException when interrupted");
            assertTrue(!interruptedWorker.isAlive(), "Interrupted thread terminated cleanly");
            testCasesPassed++;

            // Test 8: Synchronized Counter Prevents Race Conditions
            SafeCounter safeCounter = new SafeCounter();
            Thread sc1 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) safeCounter.increment();
            });
            Thread sc2 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) safeCounter.increment();
            });
            sc1.start();
            sc2.start();
            sc1.join();
            sc2.join();
            assertTrue(safeCounter.getCount() == 20000, "Synchronized SafeCounter exactly reached 20,000 (no lost updates)");
            testCasesPassed++;

            // Test 9: BLOCKED and WAITING State Observation
            Object monitorLock = new Object();
            Thread holder = new Thread(() -> {
                synchronized (monitorLock) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {}
                }
            }, "HolderThread");

            Thread blockedCandidate = new Thread(() -> {
                synchronized (monitorLock) {
                    // Contends for lock
                }
            }, "BlockedCandidateThread");

            holder.start();
            Thread.sleep(50);
            blockedCandidate.start();
            Thread.sleep(50);

            assertTrue(blockedCandidate.getState() == Thread.State.BLOCKED,
                    "Observed BLOCKED state on thread contending for synchronized monitor");
            holder.join();
            blockedCandidate.join();

            // WAITING verification
            Object waitLock = new Object();
            Thread waitingCandidate = new Thread(() -> {
                synchronized (waitLock) {
                    try {
                        waitLock.wait();
                    } catch (InterruptedException ignored) {}
                }
            }, "WaitingCandidateThread");

            waitingCandidate.start();
            Thread.sleep(50);
            assertTrue(waitingCandidate.getState() == Thread.State.WAITING,
                    "Observed WAITING state on thread calling Object.wait()");

            synchronized (waitLock) {
                waitLock.notifyAll();
            }
            waitingCandidate.join();
            testCasesPassed++;

            // Test 10: Clean Shutdown & No Deadlocks
            assertTrue(!fullProducer.isAlive(), "Full producer terminated");
            assertTrue(!emptyConsumer.isAlive(), "Empty consumer terminated");
            assertTrue(!drainWorker.isAlive(), "Drain worker terminated");
            assertTrue(!pA.isAlive() && !pB.isAlive(), "Producers terminated");
            assertTrue(!cA.isAlive() && !cB.isAlive(), "Consumers terminated");
            testCasesPassed++;

            System.out.println("\n>>> All " + testCasesPassed + " Phase 4 Test Scenarios PASSED Successfully! <<<");

        } catch (Exception e) {
            System.err.println("TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Message createMsg(String id) {
        return new Message(id, "conv_test", "sender", "content", LocalDateTime.now());
    }

    private static List<Message> createMsgBatch(String prefix, int count) {
        List<Message> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new Message(prefix + "_" + i, "conv_test", prefix, "Payload " + i, LocalDateTime.now()));
        }
        return list;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            System.err.println("  [FAIL] " + message);
            throw new AssertionError("Assertion failed: " + message);
        } else {
            System.out.println("  [PASS] " + message);
        }
    }
}
