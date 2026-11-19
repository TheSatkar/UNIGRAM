package com.unigram.threading;

/**
 * Demonstrates:
 * 1. Race conditions caused by non-atomic updates to shared mutable state without synchronization.
 * 2. Mutual exclusion using the 'synchronized' keyword to protect the critical section.
 * 3. The academic and practical difference between a Race Condition and a Deadlock.
 */
public class SynchronizationRaceDemo {

    /**
     * Unsynchronized shared counter.
     * The operation count++ consists of 3 distinct bytecode operations:
     * 1. Read count from memory into CPU register
     * 2. Increment register value
     * 3. Write register value back to memory
     * 
     * When multiple threads interleave these steps without synchronization,
     * updates are overwritten, producing inconsistent, lost updates.
     */
    public static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++; // Non-atomic read-modify-write!
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Synchronized shared counter.
     * The 'synchronized' method acquires the intrinsic lock (monitor) of 'this' object.
     * Only one thread can enter the critical section at any given time, ensuring atomicity.
     */
    public static class SafeCounter {
        private int count = 0;

        public synchronized void increment() {
            count++; // Thread-safe atomic critical section
        }

        public synchronized int getCount() {
            return count;
        }
    }

    /**
     * Runs a side-by-side demonstration of unsynchronized vs synchronized execution.
     */
    public static void runRaceConditionDemo() {
        System.out.println("======================================================================");
        System.out.println("         Demonstration: Race Conditions & Synchronization             ");
        System.out.println("======================================================================\n");

        final int iterationsPerThread = 25000;
        final int expectedTotal = iterationsPerThread * 2;

        System.out.printf("Scenario: 2 threads each incrementing a shared counter %,d times.%n", iterationsPerThread);
        System.out.printf("Expected final total: %,d%n%n", expectedTotal);

        // 1. Unsafe Counter (No Synchronization)
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        Thread unsafeThread1 = new Thread(() -> {
            for (int i = 0; i < iterationsPerThread; i++) unsafeCounter.increment();
        }, "UnsafeWorker-1");
        Thread unsafeThread2 = new Thread(() -> {
            for (int i = 0; i < iterationsPerThread; i++) unsafeCounter.increment();
        }, "UnsafeWorker-2");

        unsafeThread1.start();
        unsafeThread2.start();

        try {
            unsafeThread1.join();
            unsafeThread2.join();
        } catch (InterruptedException e) {
            System.out.println("Unsafe demo interrupted.");
        }

        int unsafeActual = unsafeCounter.getCount();
        int lostUpdates = expectedTotal - unsafeActual;
        System.out.println("--- Part A: Without Synchronization (Race Condition) ---");
        System.out.printf("  Expected Count : %,d%n", expectedTotal);
        System.out.printf("  Actual Count   : %,d%n", unsafeActual);
        System.out.printf("  Lost Updates   : %,d (Data corruption occurred!)%n%n", lostUpdates);

        // 2. Safe Counter (With Synchronization)
        SafeCounter safeCounter = new SafeCounter();
        Thread safeThread1 = new Thread(() -> {
            for (int i = 0; i < iterationsPerThread; i++) safeCounter.increment();
        }, "SafeWorker-1");
        Thread safeThread2 = new Thread(() -> {
            for (int i = 0; i < iterationsPerThread; i++) safeCounter.increment();
        }, "SafeWorker-2");

        safeThread1.start();
        safeThread2.start();

        try {
            safeThread1.join();
            safeThread2.join();
        } catch (InterruptedException e) {
            System.out.println("Safe demo interrupted.");
        }

        int safeActual = safeCounter.getCount();
        System.out.println("--- Part B: With Synchronization (Intrinsic Lock / Monitor) ---");
        System.out.printf("  Expected Count : %,d%n", expectedTotal);
        System.out.printf("  Actual Count   : %,d%n", safeActual);
        System.out.println("  Lost Updates   : 0 (100% thread-safe atomic execution)\n");

        // 3. Viva Explanation: Race Condition vs Deadlock
        System.out.println("--- Academic Comparison: Race Condition vs Deadlock ---");
        System.out.println("1. RACE CONDITION:");
        System.out.println("   - Occurs when two or more threads access shared mutable data without proper");
        System.out.println("     synchronization, and the final state depends on unpredictable thread scheduling.");
        System.out.println("   - Consequence: Inconsistent state, corrupted data, or lost updates (threads keep running).");
        System.out.println("2. DEADLOCK:");
        System.out.println("   - Occurs when two or more threads are permanently blocked, each holding a lock that");
        System.out.println("     the other needs, forming a circular dependency (e.g., Thread 1 holds Lock A waiting");
        System.out.println("     for Lock B, while Thread 2 holds Lock B waiting for Lock A).");
        System.out.println("   - Consequence: Complete standstill (threads never proceed, CPU utilization drops to 0%).");
        System.out.println("======================================================================\n");
    }
}
