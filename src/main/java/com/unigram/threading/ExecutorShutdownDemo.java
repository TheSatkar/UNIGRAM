package com.unigram.threading;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates ExecutorService shutdown lifecycle and resource hygiene:
 * 1. shutdown() — Graceful shutdown (refuses new tasks, finishes existing ones)
 * 2. shutdownNow() — Forceful shutdown (interrupts workers, drains unexecuted tasks)
 * 3. awaitTermination() — Blocking wait for complete termination
 * 4. RejectedExecutionException — Attempting to submit tasks after shutdown
 * 5. Two-phase standard Java shutdown idiom
 */
public class ExecutorShutdownDemo {

    /**
     * Executes the shutdown demonstrations.
     */
    public static void demonstrateShutdownLifecycle() {
        System.out.println("======================================================================");
        System.out.println("           ExecutorService Shutdown Lifecycle Demonstration           ");
        System.out.println("======================================================================\n");

        // 1. Graceful shutdown: shutdown() + awaitTermination()
        System.out.println("--- 1. Graceful Shutdown (shutdown() + awaitTermination()) ---");
        ExecutorService gracefulPool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        gracefulPool.submit(() -> {
            try {
                Thread.sleep(150);
                System.out.println("  [Graceful Task 1] Finished.");
            } catch (InterruptedException ignored) {}
            latch.countDown();
        });

        gracefulPool.submit(() -> {
            try {
                Thread.sleep(150);
                System.out.println("  [Graceful Task 2] Finished.");
            } catch (InterruptedException ignored) {}
            latch.countDown();
        });

        System.out.println("Calling gracefulPool.shutdown()...");
        gracefulPool.shutdown();
        System.out.printf("After shutdown(): isShutdown() = %b, isTerminated() = %b%n",
                gracefulPool.isShutdown(), gracefulPool.isTerminated());

        // Attempting to submit a new task after shutdown triggers RejectedExecutionException
        System.out.println("Attempting to submit a 3rd task after shutdown:");
        try {
            gracefulPool.submit(() -> System.out.println("Should not run"));
        } catch (RejectedExecutionException e) {
            System.out.println("  -> [✓] Caught Expected RejectedExecutionException!");
        }

        try {
            System.out.println("Calling awaitTermination(1, TimeUnit.SECONDS)...");
            boolean terminated = gracefulPool.awaitTermination(1, TimeUnit.SECONDS);
            System.out.printf("After awaitTermination(): isTerminated() = %b (Terminated in time: %b)%n%n",
                    gracefulPool.isTerminated(), terminated);
        } catch (InterruptedException e) {
            System.out.println("Interrupted during awaitTermination.");
            Thread.currentThread().interrupt();
        }

        // 2. Forceful shutdown: shutdownNow()
        System.out.println("--- 2. Forceful Shutdown (shutdownNow()) ---");
        ExecutorService forcePool = Executors.newSingleThreadExecutor();

        forcePool.submit(() -> {
            System.out.println("  [LongTask] Started. Sleeping 3000ms...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("  [LongTask] Interrupted by shutdownNow()! Exiting.");
            }
        });

        // Queue up an unexecuted task
        forcePool.submit(() -> System.out.println("  [QueuedTask] This was waiting in queue."));

        try {
            Thread.sleep(100); // Allow first task to begin running
            System.out.println("Calling forcePool.shutdownNow()...");
            List<Runnable> unexecutedTasks = forcePool.shutdownNow();

            System.out.printf("shutdownNow() returned %d unexecuted queued task(s).%n", unexecutedTasks.size());

            boolean forceTerminated = forcePool.awaitTermination(1, TimeUnit.SECONDS);
            System.out.printf("forcePool isTerminated() = %b%n%n", forceTerminated);

        } catch (InterruptedException e) {
            System.out.println("Force shutdown demo interrupted.");
            Thread.currentThread().interrupt();
        }

        // 3. Recommended Two-Phase Safe Shutdown Pattern
        System.out.println("--- 3. Recommended Robust Two-Phase Shutdown Idiom ---");
        ExecutorService samplePool = Executors.newFixedThreadPool(2);
        safeShutdown(samplePool, 500, TimeUnit.MILLISECONDS);

        System.out.println("\n>> Academic Viva Note: Shutdown Lifecycle <<");
        System.out.println("1. shutdown(): Disables new submissions. Does not interrupt existing tasks.");
        System.out.println("2. shutdownNow(): Attempts to interrupt active tasks and drains waiting tasks.");
        System.out.println("3. isShutdown(): True as soon as shutdown() or shutdownNow() is called.");
        System.out.println("4. isTerminated(): True ONLY AFTER all tasks have completely finished execution.");
        System.out.println("======================================================================\n");
    }

    /**
     * Standard Oracle/Java recommended robust two-phase shutdown idiom.
     */
    public static void safeShutdown(ExecutorService pool, long timeout, TimeUnit unit) {
        pool.shutdown(); // Phase 1: Disable new tasks
        try {
            // Wait for existing tasks to terminate
            if (!pool.awaitTermination(timeout, unit)) {
                System.out.println("  [SafeShutdown] Tasks did not finish in time; calling shutdownNow()...");
                pool.shutdownNow(); // Phase 2: Cancel currently executing tasks
                if (!pool.awaitTermination(timeout, unit)) {
                    System.err.println("  [SafeShutdown] Pool did not terminate cleanly.");
                }
            } else {
                System.out.println("  [SafeShutdown] Pool terminated gracefully within timeout.");
            }
        } catch (InterruptedException ie) {
            System.out.println("  [SafeShutdown] Shutdown interrupted; forcing shutdownNow()...");
            pool.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }
}
