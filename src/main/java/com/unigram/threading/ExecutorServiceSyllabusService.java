package com.unigram.threading;

/**
 * Coordinator service for Phase 5 ExecutorService, Callable, and Future syllabus demonstrations.
 */
public class ExecutorServiceSyllabusService {

    public void demonstrateSingleThreadExecutor() {
        ExecutorServiceBasicsDemo.demonstrateSingleThreadExecutor();
    }

    public void demonstrateFixedThreadPool() {
        ExecutorServiceBasicsDemo.demonstrateFixedThreadPool();
    }

    public void demonstrateCachedThreadPool() {
        ExecutorServiceBasicsDemo.demonstrateCachedThreadPool();
    }

    public void demonstrateExecuteVsSubmit() {
        ExecutorServiceBasicsDemo.demonstrateExecuteVsSubmit();
    }

    public void demonstrateCallableAndFuture() {
        CallableFutureDemo.demonstrateCallableAndFuture();
    }

    public void demonstrateFutureExceptions() {
        CallableFutureDemo.demonstrateFutureExceptions();
    }

    public void demonstrateCancellation() {
        FutureCancellationDemo.demonstrateCancellation();
    }

    public void demonstrateScheduling() {
        ExecutorSchedulingDemo.demonstrateScheduling();
    }

    public void demonstrateShutdownLifecycle() {
        ExecutorShutdownDemo.demonstrateShutdownLifecycle();
    }

    public void runAllDemonstrations() {
        System.out.println("======================================================================");
        System.out.println("      Running All Phase 5 ExecutorService & Future Demonstrations     ");
        System.out.println("======================================================================\n");

        demonstrateSingleThreadExecutor();
        demonstrateFixedThreadPool();
        demonstrateCachedThreadPool();
        demonstrateExecuteVsSubmit();
        demonstrateCallableAndFuture();
        demonstrateFutureExceptions();
        demonstrateCancellation();
        demonstrateScheduling();
        demonstrateShutdownLifecycle();

        System.out.println("======================================================================");
        System.out.println("         All Phase 5 Syllabus Demonstrations Completed                ");
        System.out.println("======================================================================\n");
    }
}
