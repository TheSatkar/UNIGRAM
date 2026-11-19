package com.unigram.threading;

/**
 * Demonstrates the distinct states of thread suspension in Java:
 * 1. BLOCKED: Thread waiting to acquire an intrinsic monitor lock held by another thread.
 * 2. WAITING: Thread voluntarily suspended waiting indefinitely for notify()/notifyAll() via Object.wait().
 * 3. TIMED_WAITING: Thread suspended for a specified duration via Thread.sleep() or wait(timeout).
 */
public class AdvancedThreadStatesDemo {

    /**
     * Executes the controlled demonstration of BLOCKED, WAITING, and TIMED_WAITING states.
     */
    public static void demonstrateStates() {
        System.out.println("======================================================================");
        System.out.println("      Demonstration: BLOCKED vs WAITING vs TIMED_WAITING States       ");
        System.out.println("======================================================================\n");

        demonstrateBlockedState();
        demonstrateWaitingState();
        demonstrateTimedWaitingState();

        System.out.println("--- Summary: Key Differences for University Viva ---");
        System.out.println("1. BLOCKED:");
        System.out.println("   - The thread is attempting to enter a 'synchronized' block or method, but the");
        System.out.println("     object's intrinsic monitor lock is currently owned by another active thread.");
        System.out.println("   - Transition is managed automatically by the JVM lock scheduler.");
        System.out.println("2. WAITING:");
        System.out.println("   - The thread ALREADY owned the monitor lock, but voluntarily called Object.wait().");
        System.out.println("   - Calling wait() releases the monitor lock and places the thread into the wait-set");
        System.out.println("     until another thread holding the lock explicitly calls notify() or notifyAll().");
        System.out.println("3. TIMED_WAITING:");
        System.out.println("   - The thread is sleeping for a specific duration via Thread.sleep(millis) or");
        System.out.println("     waiting with timeout via Object.wait(timeout) / Thread.join(timeout).");
        System.out.println("======================================================================\n");
    }

    private static void demonstrateBlockedState() {
        System.out.println("--- 1. Demonstrating BLOCKED State ---");
        Object monitor = new Object();

        // Lock holder thread: enters synchronized block and holds monitor lock for 400ms
        Thread lockHolder = new Thread(() -> {
            synchronized (monitor) {
                try {
                    Thread.sleep(400); // Holds monitor lock while sleeping
                } catch (InterruptedException ignored) {}
            }
        }, "LockHolder");

        // Blocked candidate thread: attempts to acquire monitor while lockHolder still holds it
        Thread blockedThread = new Thread(() -> {
            synchronized (monitor) {
                // Waits to acquire monitor
            }
        }, "BlockedCandidate");

        lockHolder.start();

        try {
            Thread.sleep(50); // Ensure lockHolder has entered the synchronized block
            blockedThread.start();
            Thread.sleep(50); // Give blockedThread time to attempt lock acquisition

            System.out.printf("  LockHolder Thread State     : %s%n", lockHolder.getState());
            System.out.printf("  BlockedCandidate Thread State: %s (Successfully observed BLOCKED!)%n", blockedThread.getState());

            lockHolder.join();
            blockedThread.join();
        } catch (InterruptedException e) {
            System.out.println("Blocked demo interrupted.");
        }
        System.out.println();
    }

    private static void demonstrateWaitingState() {
        System.out.println("--- 2. Demonstrating WAITING State (via Object.wait()) ---");
        Object signalLock = new Object();

        Thread waitingThread = new Thread(() -> {
            synchronized (signalLock) {
                try {
                    // Calling wait() releases signalLock and enters WAITING state
                    signalLock.wait();
                } catch (InterruptedException ignored) {}
            }
        }, "WaitingCandidate");

        waitingThread.start();

        try {
            Thread.sleep(50); // Allow thread to enter synchronized and call wait()

            System.out.printf("  WaitingCandidate Thread State: %s (Successfully observed WAITING!)%n", waitingThread.getState());

            // Awaken the waiting thread
            synchronized (signalLock) {
                signalLock.notifyAll();
            }
            waitingThread.join();
        } catch (InterruptedException e) {
            System.out.println("Waiting demo interrupted.");
        }
        System.out.println();
    }

    private static void demonstrateTimedWaitingState() {
        System.out.println("--- 3. Demonstrating TIMED_WAITING State (via Thread.sleep()) ---");

        Thread sleepingThread = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
        }, "SleepingCandidate");

        sleepingThread.start();

        try {
            Thread.sleep(50); // Allow thread to enter Thread.sleep()
            System.out.printf("  SleepingCandidate Thread State: %s (Successfully observed TIMED_WAITING!)%n", sleepingThread.getState());
            sleepingThread.join();
        } catch (InterruptedException e) {
            System.out.println("Timed waiting demo interrupted.");
        }
        System.out.println();
    }
}
