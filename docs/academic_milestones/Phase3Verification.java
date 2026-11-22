package com.unigram;

import com.unigram.threading.MessageStatusMonitorDaemon;
import com.unigram.threading.StudentListingBrowserTask;
import com.unigram.threading.StudentMessageSenderThread;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Automated Verification Suite for Phase 3: Thread and Runnable Fundamentals.
 * 
 * Verifies:
 * 1. Thread creation via extending Thread (StudentMessageSenderThread)
 * 2. Thread creation via implementing Runnable (StudentListingBrowserTask)
 * 3. start() executes on a distinct thread vs run() executing on caller thread
 * 4. join() correctly waits for thread termination (isAlive() becomes false)
 * 5. interrupt() wakes up sleeping thread and handles InterruptedException cleanly
 * 6. Daemon thread configuration (setDaemon(true) / isDaemon())
 * 7. Thread lifecycle state transitions (NEW, RUNNABLE, TIMED_WAITING, TERMINATED)
 * 8. Thread naming, IDs, and priority assignment
 */
public class Phase3Verification {

    public static void main(String[] args) {
        System.out.println(">>> Running Phase 3 Threading Verification Suite <<<\n");
        int testsPassed = 0;

        try {
            // Test 1: Subclassing Thread initialization
            StudentMessageSenderThread sender = new StudentMessageSenderThread("u_alice", "conv_1", 2, 50);
            assertTrue(sender.getName().equals("StudentSender-u_alice"), "Thread name initialized via super()");
            assertTrue(sender.getState() == Thread.State.NEW, "Initial thread state is NEW");
            assertTrue(!sender.isAlive(), "Thread is not alive before start()");
            testsPassed++;

            // Test 2: Implementing Runnable initialization
            StudentListingBrowserTask browserTask = new StudentListingBrowserTask("u_bob", "Electronics", 2, 50);
            assertTrue(browserTask.getStudentId().equals("u_bob"), "Runnable task studentId initialized");
            assertTrue(browserTask.getCategory().equals("Electronics"), "Runnable task category initialized");
            testsPassed++;

            // Test 3: start() executes on a separate thread
            AtomicReference<String> workerThreadName = new AtomicReference<>();
            Thread asyncThread = new Thread(() -> {
                workerThreadName.set(Thread.currentThread().getName());
            }, "TestAsyncThread");
            asyncThread.start();
            asyncThread.join();
            assertTrue(workerThreadName.get() != null && workerThreadName.get().equals("TestAsyncThread"),
                    "start() spawns execution on distinct worker thread");
            testsPassed++;

            // Test 4: run() executes synchronously on the calling thread
            AtomicReference<String> directRunThreadName = new AtomicReference<>();
            Thread directThread = new Thread(() -> {
                directRunThreadName.set(Thread.currentThread().getName());
            }, "TestDirectThread");
            directThread.run(); // Call run() directly
            assertTrue(directRunThreadName.get() != null && directRunThreadName.get().equals(Thread.currentThread().getName()),
                    "run() executes synchronously on calling (main) thread");
            testsPassed++;

            // Test 5: join() waits for thread completion
            AtomicBoolean workerDone = new AtomicBoolean(false);
            Thread sleepThread = new Thread(() -> {
                try {
                    Thread.sleep(150);
                    workerDone.set(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "SleepWorker");
            sleepThread.start();
            assertTrue(sleepThread.isAlive(), "Thread isAlive() while running");
            sleepThread.join(); // Caller waits
            assertTrue(workerDone.get(), "Worker finished before join() returned");
            assertTrue(!sleepThread.isAlive(), "Thread isAlive() is false after join()");
            assertTrue(sleepThread.getState() == Thread.State.TERMINATED, "Thread state is TERMINATED after join()");
            testsPassed++;

            // Test 6: interrupt() wakes up sleeping thread and handles InterruptedException
            AtomicBoolean caughtInterrupt = new AtomicBoolean(false);
            Thread targetToInterrupt = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    caughtInterrupt.set(true);
                }
            }, "TargetToInterrupt");
            targetToInterrupt.start();
            Thread.sleep(100); // Give thread time to enter sleep()
            targetToInterrupt.interrupt();
            targetToInterrupt.join();
            assertTrue(caughtInterrupt.get(), "interrupt() successfully triggered InterruptedException on sleeping thread");
            testsPassed++;

            // Test 7: Daemon thread configuration
            MessageStatusMonitorDaemon daemonTask = new MessageStatusMonitorDaemon(100);
            Thread daemon = new Thread(daemonTask, "TestDaemon");
            daemon.setDaemon(true);
            assertTrue(daemon.isDaemon(), "setDaemon(true) configures thread as daemon");
            daemon.start();
            daemonTask.stopMonitor();
            daemon.interrupt();
            daemon.join(500);
            testsPassed++;

            // Test 8: Thread priorities and IDs
            Thread prioThread = new Thread(() -> {}, "PrioTest");
            prioThread.setPriority(Thread.MAX_PRIORITY);
            assertTrue(prioThread.getPriority() == Thread.MAX_PRIORITY, "Thread priority set to MAX_PRIORITY (10)");
            prioThread.setPriority(Thread.MIN_PRIORITY);
            assertTrue(prioThread.getPriority() == Thread.MIN_PRIORITY, "Thread priority set to MIN_PRIORITY (1)");
            assertTrue(prioThread.threadId() > 0, "Thread assigned a valid positive threadId");
            testsPassed++;

            // Test 9: Thread lifecycle TIMED_WAITING observation
            Thread timedThread = new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            });
            timedThread.start();
            Thread.sleep(50); // allow timedThread to enter Thread.sleep()
            Thread.State observedState = timedThread.getState();
            assertTrue(observedState == Thread.State.TIMED_WAITING, "Observed TIMED_WAITING state during Thread.sleep()");
            timedThread.join();
            testsPassed++;

            System.out.println("\n>>> All " + testsPassed + " Phase 3 Threading Tests PASSED Successfully! <<<");

        } catch (Exception e) {
            System.err.println("TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (!condition) {
            System.err.println("FAILED: " + testName);
            throw new AssertionError("Test assertion failed: " + testName);
        } else {
            System.out.println("  [PASS] " + testName);
        }
    }
}
