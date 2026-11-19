package com.unigram.threading;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates Future task cancellation and cooperative interruption:
 * 1. Submitting a long-running task and obtaining Future<?>
 * 2. Cancelling via future.cancel(true)
 * 3. Cooperative interruption handling in the running task
 * 4. Verifying future.isCancelled() and future.isDone()
 * 5. Catching CancellationException on future.get()
 */
public class FutureCancellationDemo {

    /**
     * Executes the task cancellation demonstration.
     */
    public static void demonstrateCancellation() {
        System.out.println("======================================================================");
        System.out.println("           Future Cancellation & Interruption Demonstration           ");
        System.out.println("======================================================================\n");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskStartedLatch = new CountDownLatch(1);

        try {
            System.out.println("Submitting long-running message-indexing task (intended for 3000ms)...");
            Future<String> future = executor.submit(() -> {
                System.out.println("  [IndexingTask] Task started execution.");
                taskStartedLatch.countDown(); // Signal main thread that task is actively running

                for (int i = 1; i <= 30; i++) {
                    // Cooperative interruption check
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("  [IndexingTask] Detected interrupt flag! Aborting gracefully.");
                        return "Interrupted early";
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("  [IndexingTask] Caught InterruptedException during sleep! Aborting.");
                        // Restore interrupt flag or return
                        Thread.currentThread().interrupt();
                        return "Interrupted during sleep";
                    }
                }
                return "Completed 100% Indexing";
            });

            // Ensure task has actually entered execution before attempting cancellation
            taskStartedLatch.await(2, TimeUnit.SECONDS);
            System.out.printf("Before cancel: isDone() = %b, isCancelled() = %b%n",
                    future.isDone(), future.isCancelled());

            System.out.println("\nCalling future.cancel(true) (mayInterruptIfRunning = true)...");
            boolean cancelSuccess = future.cancel(true);
            System.out.println("future.cancel(true) returned: " + cancelSuccess);

            System.out.printf("After cancel: isDone() = %b, isCancelled() = %b%n",
                    future.isDone(), future.isCancelled());

            System.out.println("\nAttempting to call future.get() on the cancelled task:");
            try {
                future.get();
                System.out.println("Unexpected: get() completed without exception.");
            } catch (CancellationException e) {
                System.out.println("[✓] Caught Expected CancellationException when calling get() on cancelled future!");
            } catch (ExecutionException e) {
                System.out.println("ExecutionException: " + e.getMessage());
            }

            System.out.println("\n>> Academic Viva Note: Future.cancel(boolean mayInterruptIfRunning) <<");
            System.out.println("1. If the task has not started yet, it is removed from the work queue and will never run.");
            System.out.println("2. If mayInterruptIfRunning is true, Java calls thread.interrupt() on the worker thread.");
            System.out.println("3. Cancellation is COOPERATIVE: if the task does not check for interrupts or");
            System.out.println("   catch InterruptedException, it may continue running to completion.");
            System.out.println("4. Once cancelled, calling future.get() always throws CancellationException.");

        } catch (InterruptedException e) {
            System.out.println("Cancellation demo interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
        System.out.println();
    }
}
