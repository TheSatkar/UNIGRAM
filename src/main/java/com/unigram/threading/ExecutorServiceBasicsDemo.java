package com.unigram.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates Executor and ExecutorService fundamentals:
 * 1. SingleThreadExecutor (sequential execution with 1 worker thread)
 * 2. FixedThreadPool (fixed number of concurrent worker threads reused for tasks)
 * 3. CachedThreadPool (dynamically expanding/reusing threads for bursts)
 * 4. execute() vs submit() comparison
 */
public class ExecutorServiceBasicsDemo {

    /**
     * Demo A: Single-Thread Executor.
     * Demonstrates that tasks execute strictly sequentially on a single dedicated worker thread.
     */
    public static void demonstrateSingleThreadExecutor() {
        System.out.println("======================================================================");
        System.out.println("           A. Single-Thread Executor Demonstration                    ");
        System.out.println("======================================================================\n");

        ExecutorService singleExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "UniGram-SingleWorker"));
        final int taskCount = 3;
        CountDownLatch latch = new CountDownLatch(taskCount);

        try {
            System.out.println("Submitting 3 tasks to Executors.newSingleThreadExecutor()...");
            for (int i = 1; i <= taskCount; i++) {
                final int taskId = i;
                singleExecutor.execute(() -> {
                    System.out.printf("  [Task %d] Started on thread: %s%n", taskId, Thread.currentThread().getName());
                    try {
                        Thread.sleep(150); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.printf("  [Task %d] Finished on thread: %s%n", taskId, Thread.currentThread().getName());
                    latch.countDown();
                });
            }

            // Wait for all tasks to complete
            latch.await(3, TimeUnit.SECONDS);

            System.out.println("\n>> Academic Viva Note: SingleThreadExecutor <<");
            System.out.println("All tasks executed sequentially on the exact same worker thread.");
            System.out.println("If this single thread dies during task execution, the executor creates");
            System.out.println("a new thread to take its place for subsequent tasks.");

        } catch (InterruptedException e) {
            System.out.println("SingleThread demo interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            singleExecutor.shutdown();
        }
        System.out.println();
    }

    /**
     * Demo B: Fixed-Thread Pool.
     * Demonstrates concurrency using a bounded pool of reusable threads.
     */
    public static void demonstrateFixedThreadPool() {
        System.out.println("======================================================================");
        System.out.println("           B. Fixed-Thread Pool Demonstration (3 Workers)             ");
        System.out.println("======================================================================\n");

        final int poolSize = 3;
        final int taskCount = 6;
        ExecutorService fixedPool = Executors.newFixedThreadPool(poolSize, new ThreadFactoryWithPrefix("UniGram-Fixed"));
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<String> workerNames = Collections.synchronizedSet(new HashSet<>());

        try {
            System.out.printf("Submitting %d tasks to a FixedThreadPool of size %d...%n", taskCount, poolSize);
            for (int i = 1; i <= taskCount; i++) {
                final int taskId = i;
                fixedPool.execute(() -> {
                    String threadName = Thread.currentThread().getName();
                    workerNames.add(threadName);
                    System.out.printf("  [Task %d] Executing concurrently on: %s%n", taskId, threadName);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    latch.countDown();
                });
            }

            latch.await(5, TimeUnit.SECONDS);

            System.out.printf("%nAll %d tasks completed. Distinct worker threads utilized: %d (%s)%n",
                    taskCount, workerNames.size(), workerNames);
            System.out.println(">> Academic Viva Note: FixedThreadPool <<");
            System.out.println("Threads are reused. Once the 3 worker threads are busy, additional");
            System.out.println("submitted tasks wait in an internal unbounded LinkedBlockingQueue.");

        } catch (InterruptedException e) {
            System.out.println("FixedThreadPool demo interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            fixedPool.shutdown();
        }
        System.out.println();
    }

    /**
     * Demo C: Cached-Thread Pool.
     * Demonstrates dynamic thread creation and reuse for short-lived tasks.
     */
    public static void demonstrateCachedThreadPool() {
        System.out.println("======================================================================");
        System.out.println("           C. Cached-Thread Pool Demonstration                        ");
        System.out.println("======================================================================\n");

        ExecutorService cachedPool = Executors.newCachedThreadPool(new ThreadFactoryWithPrefix("UniGram-Cached"));
        final int burstCount = 4;
        CountDownLatch burstLatch = new CountDownLatch(burstCount);

        try {
            System.out.printf("Submitting burst of %d simultaneous tasks...%n", burstCount);
            for (int i = 1; i <= burstCount; i++) {
                final int id = i;
                cachedPool.execute(() -> {
                    System.out.printf("  [Burst Task %d] Running on: %s%n", id, Thread.currentThread().getName());
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    burstLatch.countDown();
                });
            }

            burstLatch.await(3, TimeUnit.SECONDS);

            System.out.println("\nPausing 200ms so worker threads become idle and available for reuse...");
            Thread.sleep(200);

            // Submit subsequent task: should reuse an existing idle thread rather than creating a new one
            CountDownLatch followUpLatch = new CountDownLatch(1);
            cachedPool.execute(() -> {
                System.out.printf("  [Follow-up Task] Reused idle worker: %s%n", Thread.currentThread().getName());
                followUpLatch.countDown();
            });

            followUpLatch.await(2, TimeUnit.SECONDS);

            System.out.println("\n>> Academic Viva Note: CachedThreadPool <<");
            System.out.println("Uses a SynchronousQueue internally. Creates new threads as needed,");
            System.out.println("but reuses previously constructed threads when they are available.");
            System.out.println("Idle threads terminate after being unused for 60 seconds.");

        } catch (InterruptedException e) {
            System.out.println("CachedThreadPool demo interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            cachedPool.shutdown();
        }
        System.out.println();
    }

    /**
     * Demo D: execute() vs submit().
     */
    public static void demonstrateExecuteVsSubmit() {
        System.out.println("======================================================================");
        System.out.println("           D. execute() versus submit() Comparison                    ");
        System.out.println("======================================================================\n");

        ExecutorService executor = Executors.newFixedThreadPool(2, new ThreadFactoryWithPrefix("UniGram-Worker"));

        try {
            // 1. execute(Runnable): Fire-and-forget, void return
            System.out.println("1. Calling execute(Runnable):");
            System.out.println("   - Defined in Executor interface.");
            System.out.println("   - Returns void (fire-and-forget). Cannot track result or handle exceptions via caller.");
            executor.execute(() -> {
                System.out.println("   -> [execute(Runnable)] Running in background.");
            });

            // 2. submit(Runnable): Returns Future<?>
            System.out.println("\n2. Calling submit(Runnable):");
            System.out.println("   - Defined in ExecutorService interface.");
            System.out.println("   - Returns Future<?>. future.get() returns null upon successful completion.");
            Future<?> runnableFuture = executor.submit(() -> {
                System.out.println("   -> [submit(Runnable)] Running in background.");
            });
            Object runnableResult = runnableFuture.get(2, TimeUnit.SECONDS);
            System.out.println("   -> runnableFuture.get() returned: " + runnableResult + " (Expected: null)");

            // 3. submit(Callable<T>): Returns Future<T> with actual value
            System.out.println("\n3. Calling submit(Callable<T>):");
            System.out.println("   - Defined in ExecutorService interface.");
            System.out.println("   - Callable returns a value and can throw checked exceptions.");
            System.out.println("   - Returns Future<T>. future.get() yields the computed value.");
            Future<String> callableFuture = executor.submit(() -> {
                System.out.println("   -> [submit(Callable)] Computing result...");
                return "Student Message Validated Successfully";
            });
            String result = callableFuture.get(2, TimeUnit.SECONDS);
            System.out.println("   -> callableFuture.get() returned: \"" + result + "\"");

        } catch (Exception e) {
            System.out.println("Error in execute vs submit demo: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
        System.out.println();
    }

    /**
     * Custom ThreadFactory providing readable names.
     */
    public static class ThreadFactoryWithPrefix implements java.util.concurrent.ThreadFactory {
        private final String prefix;
        private int count = 1;

        public ThreadFactoryWithPrefix(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public synchronized Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-Worker-" + (count++));
        }
    }
}
