package com.unigram;

import com.unigram.exception.InvalidMessageException;
import com.unigram.model.Message;
import com.unigram.threading.CallableFutureDemo.FailingValidationTask;
import com.unigram.threading.CallableFutureDemo.MessageCounterTask;
import com.unigram.threading.CallableFutureDemo.UnreadCalculatorTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated Verification Suite for Phase 5: ExecutorService, Callable, and Future.
 * 
 * Verifies all 15 syllabus scenarios:
 * 1. Single-thread executor executes all submitted tasks sequentially
 * 2. Fixed thread pool executes all submitted tasks concurrently
 * 3. Worker threads are reused appropriately
 * 4. Callable returns expected result
 * 5. Future retrieves correct result
 * 6. Future completion is correctly reported (isDone)
 * 7. ExecutionException is handled correctly with unwrapped cause
 * 8. Future cancellation is handled correctly (isCancelled, CancellationException)
 * 9. Interrupted tasks terminate cooperatively
 * 10. Delayed scheduled task executes
 * 11. Periodic scheduling executes controlled number of times
 * 12. Scheduled task cancellation works
 * 13. Graceful executor shutdown terminates workers
 * 14. Tasks submitted after shutdown are rejected (RejectedExecutionException)
 * 15. No executor worker threads remain unexpectedly alive
 */
public class Phase5Verification {

    private static int totalAssertionsPassed = 0;

    public static void main(String[] args) {
        System.out.println(">>> Running Phase 5 ExecutorService & Future Verification Suite <<<\n");
        int testScenariosPassed = 0;

        try {
            // Scenario 1: Single-thread executor executes all submitted tasks
            ExecutorService singleExec = Executors.newSingleThreadExecutor();
            try {
                AtomicInteger singleCounter = new AtomicInteger(0);
                CountDownLatch singleLatch = new CountDownLatch(3);
                for (int i = 0; i < 3; i++) {
                    singleExec.execute(() -> {
                        singleCounter.incrementAndGet();
                        singleLatch.countDown();
                    });
                }
                boolean finished = singleLatch.await(2, TimeUnit.SECONDS);
                assertTrue(finished, "Single-thread executor completed all tasks within timeout");
                assertTrue(singleCounter.get() == 3, "Single-thread executor processed exactly 3 tasks");
                testScenariosPassed++;
            } finally {
                singleExec.shutdown();
                singleExec.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 2: Fixed thread pool executes all submitted tasks
            ExecutorService fixedPool = Executors.newFixedThreadPool(3);
            try {
                AtomicInteger fixedCounter = new AtomicInteger(0);
                CountDownLatch fixedLatch = new CountDownLatch(6);
                for (int i = 0; i < 6; i++) {
                    fixedPool.execute(() -> {
                        fixedCounter.incrementAndGet();
                        fixedLatch.countDown();
                    });
                }
                boolean finished = fixedLatch.await(3, TimeUnit.SECONDS);
                assertTrue(finished, "Fixed-thread pool completed all tasks within timeout");
                assertTrue(fixedCounter.get() == 6, "Fixed-thread pool processed exactly 6 tasks");
                testScenariosPassed++;
            } finally {
                fixedPool.shutdown();
                fixedPool.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 3: Worker threads are reused appropriately
            ExecutorService reusePool = Executors.newFixedThreadPool(2);
            try {
                Set<String> workerNames = Collections.synchronizedSet(new HashSet<>());
                CountDownLatch reuseLatch = new CountDownLatch(6);
                for (int i = 0; i < 6; i++) {
                    reusePool.execute(() -> {
                        workerNames.add(Thread.currentThread().getName());
                        reuseLatch.countDown();
                    });
                }
                reuseLatch.await(2, TimeUnit.SECONDS);
                assertTrue(workerNames.size() <= 2 && workerNames.size() > 0,
                        "Fixed pool of size 2 reused workers for 6 tasks (Distinct threads: " + workerNames.size() + ")");
                testScenariosPassed++;
            } finally {
                reusePool.shutdown();
                reusePool.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 4: Callable returns expected result
            List<Message> sampleMessages = createSampleDataset();
            ExecutorService callablePool = Executors.newFixedThreadPool(2);
            try {
                MessageCounterTask counterTask = new MessageCounterTask("conv_101", sampleMessages);
                Future<Integer> countFuture = callablePool.submit(counterTask);
                Integer count = countFuture.get(2, TimeUnit.SECONDS);
                assertTrue(count == 3, "MessageCounterTask returned correct count (3)");
                testScenariosPassed++;

                // Scenario 5: Future retrieves correct result for unread calculation
                UnreadCalculatorTask unreadTask = new UnreadCalculatorTask("usr_maya", sampleMessages);
                Future<Integer> unreadFuture = callablePool.submit(unreadTask);
                Integer unread = unreadFuture.get(2, TimeUnit.SECONDS);
                assertTrue(unread == 3, "UnreadCalculatorTask returned correct unread count (3)");
                testScenariosPassed++;

                // Scenario 6: Future completion is correctly reported (isDone)
                assertTrue(countFuture.isDone(), "countFuture.isDone() reports true after completion");
                assertTrue(unreadFuture.isDone(), "unreadFuture.isDone() reports true after completion");
                testScenariosPassed++;
            } finally {
                callablePool.shutdown();
                callablePool.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 7: ExecutionException is handled correctly with unwrapped cause
            ExecutorService exService = Executors.newSingleThreadExecutor();
            try {
                Future<Boolean> failingFuture = exService.submit(new FailingValidationTask("   "));
                boolean caughtExecEx = false;
                boolean causeMatched = false;
                try {
                    failingFuture.get(2, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    caughtExecEx = true;
                    if (e.getCause() instanceof InvalidMessageException) {
                        causeMatched = true;
                    }
                }
                assertTrue(caughtExecEx, "ExecutionException thrown by Future.get() for failing Callable");
                assertTrue(causeMatched, "Unwrapped cause matches expected InvalidMessageException");
                testScenariosPassed++;
            } finally {
                exService.shutdown();
                exService.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 8: Future cancellation is handled correctly (isCancelled, CancellationException)
            ExecutorService cancelPool = Executors.newSingleThreadExecutor();
            try {
                CountDownLatch cancelTaskStarted = new CountDownLatch(1);
                Future<String> longFuture = cancelPool.submit(() -> {
                    cancelTaskStarted.countDown();
                    Thread.sleep(5000);
                    return "Completed";
                });

                cancelTaskStarted.await(2, TimeUnit.SECONDS);
                boolean cancelled = longFuture.cancel(true);
                assertTrue(cancelled, "Future.cancel(true) returned true");
                assertTrue(longFuture.isCancelled(), "Future.isCancelled() reports true");
                assertTrue(longFuture.isDone(), "Future.isDone() reports true after cancellation");

                boolean caughtCancelEx = false;
                try {
                    longFuture.get();
                } catch (CancellationException e) {
                    caughtCancelEx = true;
                }
                assertTrue(caughtCancelEx, "Future.get() throws CancellationException after cancellation");
                testScenariosPassed++;
            } finally {
                cancelPool.shutdown();
                cancelPool.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 9: Interrupted tasks terminate cooperatively
            ExecutorService coopPool = Executors.newSingleThreadExecutor();
            try {
                AtomicBoolean coopInterrupted = new AtomicBoolean(false);
                CountDownLatch coopStarted = new CountDownLatch(1);
                Future<?> coopFuture = coopPool.submit(() -> {
                    coopStarted.countDown();
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            coopInterrupted.set(true);
                            Thread.currentThread().interrupt(); // cooperate and exit
                            break;
                        }
                    }
                });

                coopStarted.await(2, TimeUnit.SECONDS);
                Thread.sleep(15); // ensure worker thread has entered loop and is sleeping
                coopFuture.cancel(true);
                coopPool.shutdown();
                boolean terminated = coopPool.awaitTermination(2, TimeUnit.SECONDS);
                assertTrue(terminated, "Cooperative worker terminated cleanly upon cancellation interrupt");
                assertTrue(coopInterrupted.get(), "Worker thread caught InterruptedException and exited cooperatively");
                testScenariosPassed++;
            } finally {
                coopPool.shutdownNow();
            }

            // Scenario 10: Delayed scheduled task executes
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            try {
                CountDownLatch delayLatch = new CountDownLatch(1);
                long scheduleTime = System.currentTimeMillis();
                ScheduledFuture<?> delayFuture = scheduler.schedule(delayLatch::countDown, 150, TimeUnit.MILLISECONDS);
                boolean delayPassed = delayLatch.await(2, TimeUnit.SECONDS);
                long elapsed = System.currentTimeMillis() - scheduleTime;
                assertTrue(delayPassed, "Delayed scheduled task executed within timeout");
                assertTrue(elapsed >= 120, "Task waited for specified delay (elapsed: " + elapsed + "ms)");
                assertTrue(delayFuture.isDone(), "delayFuture.isDone() is true after execution");
                testScenariosPassed++;

                // Scenario 11: Periodic scheduling executes controlled number of times
                AtomicInteger periodicCounter = new AtomicInteger(0);
                CountDownLatch periodicLatch = new CountDownLatch(3);
                ScheduledFuture<?> rateFuture = scheduler.scheduleAtFixedRate(() -> {
                    periodicCounter.incrementAndGet();
                    periodicLatch.countDown();
                }, 20, 80, TimeUnit.MILLISECONDS);

                boolean rateDone = periodicLatch.await(2, TimeUnit.SECONDS);
                assertTrue(rateDone, "Periodic task executed at least 3 times within timeout");
                assertTrue(periodicCounter.get() >= 3, "Periodic task reached target execution count (>= 3)");
                testScenariosPassed++;

                // Scenario 12: Scheduled task cancellation works
                boolean cancelScheduled = rateFuture.cancel(true);
                assertTrue(cancelScheduled, "ScheduledFuture.cancel(true) succeeded");
                assertTrue(rateFuture.isCancelled(), "rateFuture.isCancelled() is true");
                int countAfterCancel = periodicCounter.get();
                Thread.sleep(150); // Wait another period
                assertTrue(periodicCounter.get() == countAfterCancel, "Periodic task ceased running after cancellation");
                testScenariosPassed++;
            } finally {
                scheduler.shutdown();
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            }

            // Scenario 13: Graceful executor shutdown terminates workers
            ExecutorService gracefulPool = Executors.newFixedThreadPool(2);
            CountDownLatch taskLatch = new CountDownLatch(2);
            gracefulPool.submit(() -> {
                try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                taskLatch.countDown();
            });
            gracefulPool.submit(() -> {
                try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                taskLatch.countDown();
            });
            gracefulPool.shutdown();
            assertTrue(gracefulPool.isShutdown(), "gracefulPool.isShutdown() reports true immediately");
            boolean terminatedGracefully = gracefulPool.awaitTermination(2, TimeUnit.SECONDS);
            assertTrue(terminatedGracefully, "gracefulPool terminated within timeout");
            assertTrue(gracefulPool.isTerminated(), "gracefulPool.isTerminated() reports true");
            testScenariosPassed++;

            // Scenario 14: Tasks submitted after shutdown are rejected appropriately
            boolean rejectedCaught = false;
            try {
                gracefulPool.submit(() -> System.out.println("Rejected task"));
            } catch (RejectedExecutionException e) {
                rejectedCaught = true;
            }
            assertTrue(rejectedCaught, "Submitting task after shutdown() throws RejectedExecutionException");
            testScenariosPassed++;

            // Scenario 15: No executor worker threads remain unexpectedly alive
            assertTrue(singleExec.isTerminated(), "Single-thread executor terminated");
            assertTrue(fixedPool.isTerminated(), "Fixed-thread pool terminated");
            assertTrue(reusePool.isTerminated(), "Reuse-thread pool terminated");
            assertTrue(callablePool.isTerminated(), "Callable pool terminated");
            assertTrue(exService.isTerminated(), "Exception pool terminated");
            assertTrue(cancelPool.isTerminated(), "Cancel pool terminated");
            assertTrue(scheduler.isTerminated(), "Scheduler pool terminated");
            assertTrue(gracefulPool.isTerminated(), "Graceful pool terminated");
            testScenariosPassed++;

            System.out.printf("%n>>> All %d Phase 5 Test Scenarios PASSED (%d Total Assertions Checked) Successfully! <<<%n",
                    testScenariosPassed, totalAssertionsPassed);

        } catch (Exception e) {
            System.err.println("PHASE 5 TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<Message> createSampleDataset() {
        List<Message> list = new ArrayList<>();
        list.add(new Message("m1", "conv_101", "usr_alex", "Payload 1", LocalDateTime.now()));
        list.add(new Message("m2", "conv_101", "usr_maya", "Payload 2", LocalDateTime.now()));
        list.add(new Message("m3", "conv_101", "usr_alex", "Payload 3", LocalDateTime.now()));
        list.add(new Message("m4", "conv_102", "usr_alex", "Payload 4", LocalDateTime.now()));
        return list;
    }

    private static void assertTrue(boolean condition, String message) {
        totalAssertionsPassed++;
        if (!condition) {
            System.err.println("  [FAIL] " + message);
            throw new AssertionError("Assertion failed: " + message);
        } else {
            System.out.println("  [PASS] " + message);
        }
    }
}
