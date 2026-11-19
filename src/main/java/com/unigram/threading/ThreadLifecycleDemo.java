package com.unigram.threading;

/**
 * Demonstrates the Java Thread Lifecycle states (Thread.State):
 * - NEW: Created but not yet started
 * - RUNNABLE: Executing in JVM or eligible to run by OS scheduler
 * - TIMED_WAITING: Sleeping or waiting for a specified timeout (Thread.sleep)
 * - TERMINATED: Completed execution
 * 
 * Note: BLOCKED (waiting for monitor lock) and WAITING (waiting indefinitely via wait())
 * are introduced in Phase 4 with synchronization and producer-consumer primitives.
 */
public class ThreadLifecycleDemo {

    /**
     * Demonstrates observable state transitions of a worker thread.
     */
    public static void demonstrateLifecycleStates() {
        System.out.println("======================================================================");
        System.out.println("            Thread Lifecycle States Demonstration                     ");
        System.out.println("======================================================================\n");

        // 1. NEW State: Instantiated but start() has NOT been called
        Thread worker = new Thread(() -> {
            try {
                System.out.printf("  [Worker Action] Worker running. Entering TIMED_WAITING for 400ms...%n");
                Thread.sleep(400); // Enters TIMED_WAITING
                System.out.printf("  [Worker Action] Worker awoke from sleep. Finishing run() method.%n");
            } catch (InterruptedException e) {
                System.out.println("  [Worker Action] Worker interrupted!");
            }
        }, "Lifecycle-Worker-1");

        System.out.println("1. State after creation (before start()):");
        System.out.printf("   Thread Name: %s | State: %s | isAlive: %b%n%n",
                worker.getName(), worker.getState(), worker.isAlive());

        // 2. RUNNABLE State: Calling start() transitions thread to RUNNABLE
        worker.start();
        System.out.println("2. State immediately after calling start():");
        System.out.printf("   Thread Name: %s | State: %s | isAlive: %b%n%n",
                worker.getName(), worker.getState(), worker.isAlive());

        // 3. TIMED_WAITING State: Allow the worker to enter Thread.sleep()
        try {
            // Main thread sleeps briefly (100ms) to give worker time to start and call Thread.sleep(400ms)
            Thread.sleep(100);
            System.out.println("3. State while worker is inside Thread.sleep(400ms):");
            System.out.printf("   Thread Name: %s | State: %s | isAlive: %b%n%n",
                    worker.getName(), worker.getState(), worker.isAlive());

            // 4. WAITING for termination via join()
            System.out.println("4. Main thread calling worker.join() to wait for its termination...");
            worker.join(); // Main thread waits until worker terminates
            System.out.println("   worker.join() returned. Worker thread has terminated.\n");

        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted during demonstration.");
        }

        // 5. TERMINATED State: run() has returned
        System.out.println("5. State after run() completes:");
        System.out.printf("   Thread Name: %s | State: %s | isAlive: %b%n%n",
                worker.getName(), worker.getState(), worker.isAlive());

        System.out.println(">> Academic Note on Thread Scheduling <<");
        System.out.println("Thread state transitions depend on the underlying Operating System");
        System.out.println("thread scheduler. Exact timing of state observation is non-deterministic,");
        System.out.println("which is why synchronization primitives (Phase 4) are vital for coordination.\n");
    }
}
