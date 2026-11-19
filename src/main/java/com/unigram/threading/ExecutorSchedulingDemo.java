package com.unigram.threading;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates ScheduledExecutorService:
 * 1. schedule() for delayed one-shot execution
 * 2. scheduleAtFixedRate() for fixed-interval periodic execution
 * 3. scheduleWithFixedDelay() for fixed-delay execution following task completion
 * 4. Cancelling scheduled futures and executor termination
 */
public class ExecutorSchedulingDemo {

    /**
     * Executes the scheduling demonstrations with controlled iterations.
     */
    public static void demonstrateScheduling() {
        System.out.println("======================================================================");
        System.out.println("           ScheduledExecutorService Demonstration                     ");
        System.out.println("======================================================================\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        try {
            // 1. One-shot delayed task: schedule()
            System.out.println("1. One-shot Delayed Task via scheduler.schedule():");
            System.out.println("   Scheduling task to execute once after a 300ms delay...");
            long scheduleTime = System.currentTimeMillis();
            CountDownLatch oneShotLatch = new CountDownLatch(1);

            ScheduledFuture<?> oneShotFuture = scheduler.schedule(() -> {
                long elapsed = System.currentTimeMillis() - scheduleTime;
                System.out.printf("   -> [One-shot Task] Executed after %d ms delay on thread: %s%n",
                        elapsed, Thread.currentThread().getName());
                oneShotLatch.countDown();
            }, 300, TimeUnit.MILLISECONDS);

            oneShotLatch.await(2, TimeUnit.SECONDS);

            // 2. Periodic execution: scheduleAtFixedRate()
            System.out.println("\n2. Periodic Task via scheduler.scheduleAtFixedRate():");
            System.out.println("   Scheduling task to run every 150ms regardless of execution duration (controlled 3 runs)...");
            AtomicInteger fixedRateCount = new AtomicInteger(0);
            CountDownLatch fixedRateLatch = new CountDownLatch(3);

            ScheduledFuture<?> fixedRateFuture = scheduler.scheduleAtFixedRate(() -> {
                int runNumber = fixedRateCount.incrementAndGet();
                System.out.printf("   -> [FixedRate Task #%d] Executed at %d ms%n",
                        runNumber, System.currentTimeMillis() % 10000);
                fixedRateLatch.countDown();
            }, 50, 150, TimeUnit.MILLISECONDS);

            fixedRateLatch.await(3, TimeUnit.SECONDS);
            fixedRateFuture.cancel(true); // Cancel periodic future after 3 runs
            System.out.println("   -> Cancelled fixedRateFuture successfully.");

            // 3. Periodic execution with delay after completion: scheduleWithFixedDelay()
            System.out.println("\n3. Task via scheduler.scheduleWithFixedDelay():");
            System.out.println("   Next execution starts 150ms AFTER previous execution completes (controlled 3 runs)...");
            AtomicInteger fixedDelayCount = new AtomicInteger(0);
            CountDownLatch fixedDelayLatch = new CountDownLatch(3);

            ScheduledFuture<?> fixedDelayFuture = scheduler.scheduleWithFixedDelay(() -> {
                int runNumber = fixedDelayCount.incrementAndGet();
                System.out.printf("   -> [FixedDelay Task #%d] Started. Simulating 50ms work...%n", runNumber);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.printf("   -> [FixedDelay Task #%d] Completed. Next run starts 150ms from now.%n", runNumber);
                fixedDelayLatch.countDown();
            }, 50, 150, TimeUnit.MILLISECONDS);

            fixedDelayLatch.await(3, TimeUnit.SECONDS);
            fixedDelayFuture.cancel(true); // Cancel periodic future after 3 runs
            System.out.println("   -> Cancelled fixedDelayFuture successfully.");

            System.out.println("\n>> Academic Viva Note: FixedRate vs FixedDelay <<");
            System.out.println("1. scheduleAtFixedRate: Fires at t0, t0 + period, t0 + 2*period.");
            System.out.println("   If a task takes longer than period, the next execution starts immediately");
            System.out.println("   (concurrent executions of the same task do not overlap).");
            System.out.println("2. scheduleWithFixedDelay: The clock for the delay interval starts ONLY AFTER");
            System.out.println("   the prior task's execution finishes completely.");

        } catch (InterruptedException e) {
            System.out.println("Scheduling demo interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            scheduler.shutdown();
        }
        System.out.println();
    }
}
