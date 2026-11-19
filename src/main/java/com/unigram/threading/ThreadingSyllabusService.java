package com.unigram.threading;

/**
 * Coordinator for all Phase 3 Threading Syllabus Demonstrations.
 * 
 * Provides isolated, clear, and academically sound demonstrations of:
 * 1. Extending Thread vs Implementing Runnable
 * 2. start() vs run()
 * 3. sleep() and join()
 * 4. Thread interruption and InterruptedException handling
 * 5. Thread lifecycle states (NEW, RUNNABLE, TIMED_WAITING, TERMINATED)
 * 6. Daemon vs Non-Daemon thread behavior
 * 7. Thread priorities, names, and IDs
 */
public class ThreadingSyllabusService {

    /**
     * Demo 1: Extending Thread vs Implementing Runnable.
     */
    public void demonstrateThreadVsRunnable() {
        System.out.println("======================================================================");
        System.out.println("     1. Extending Thread vs Implementing Runnable                     ");
        System.out.println("======================================================================\n");

        System.out.println("Approach A: Extending java.lang.Thread");
        System.out.println("- Inherits from Thread class; limits class inheritance (Java single inheritance).");
        StudentMessageSenderThread senderThread = new StudentMessageSenderThread(
                "usr_student_A", "conv_demo_101", 2, 200);
        senderThread.start();

        System.out.println("\nApproach B: Implementing java.lang.Runnable");
        System.out.println("- Separates task logic from thread execution (Best Practice / Composition).");
        StudentListingBrowserTask browserTask = new StudentListingBrowserTask(
                "usr_student_B", "Textbooks", 2, 200);
        Thread browserThread = new Thread(browserTask, "ListingBrowser-usr_student_B");
        browserThread.start();

        // Wait for both to finish before concluding demo
        try {
            senderThread.join();
            browserThread.join();
        } catch (InterruptedException e) {
            System.out.println("Demonstration interrupted.");
        }
        System.out.println("\n[✓] Thread vs Runnable demonstration complete.\n");
    }

    /**
     * Demo 2: start() vs run().
     */
    public void demonstrateStartVsRun() {
        System.out.println("======================================================================");
        System.out.println("     2. start() vs run() Comparison                                   ");
        System.out.println("======================================================================\n");

        Runnable task = () -> {
            System.out.printf("  -> Task executing inside thread: '%s' (ID: %d)%n",
                    Thread.currentThread().getName(), Thread.currentThread().threadId());
        };

        Thread thread1 = new Thread(task, "Direct-Run-Thread");
        Thread thread2 = new Thread(task, "Async-Start-Thread");

        System.out.printf("Current Main Thread: '%s' (ID: %d)%n%n",
                Thread.currentThread().getName(), Thread.currentThread().threadId());

        System.out.println("Case A: Calling run() DIRECTLY on thread object:");
        System.out.println("Calling thread1.run() DOES NOT spawn a new thread. It executes synchronously on caller thread:");
        thread1.run(); // Synchronous call on main thread

        System.out.println("\nCase B: Calling start() on thread object:");
        System.out.println("Calling thread2.start() allocates a new execution stack and runs asynchronously:");
        thread2.start(); // Spawns new thread

        try {
            thread2.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted waiting for thread2.");
        }
        System.out.println("\n[✓] start() vs run() comparison complete.\n");
    }

    /**
     * Demo 3: sleep() and join().
     */
    public void demonstrateSleepAndJoin() {
        System.out.println("======================================================================");
        System.out.println("     3. sleep() and join() Coordination                               ");
        System.out.println("======================================================================\n");

        System.out.println("Spawning two student sender threads concurrently...");
        StudentMessageSenderThread studentAlex = new StudentMessageSenderThread(
                "Alex", "conv_101", 3, 250);
        StudentMessageSenderThread studentMaya = new StudentMessageSenderThread(
                "Maya", "conv_101", 3, 250);

        long startTime = System.currentTimeMillis();

        studentAlex.start();
        studentMaya.start();

        System.out.println("Main thread calling join() on both sender threads.");
        System.out.println("Main thread will suspend execution until BOTH student threads finish...");

        try {
            studentAlex.join();
            studentMaya.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted while waiting for senders.");
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.printf("\nBoth student threads finished. Main thread resumed after %d ms.%n", elapsedTime);
        System.out.println("Notice that both threads executed concurrently (interleaved output).");
        System.out.println("\n[✓] sleep() and join() demonstration complete.\n");
    }

    /**
     * Demo 4: Thread Interruption and InterruptedException handling.
     */
    public void demonstrateInterruption() {
        System.out.println("======================================================================");
        System.out.println("     4. Thread Interruption & InterruptedException Handling           ");
        System.out.println("======================================================================\n");

        Thread longRunningSender = new Thread(() -> {
            System.out.println("  [LongSender] Thread started. Entering long simulated upload (sleep for 3000ms)...");
            try {
                Thread.sleep(3000);
                System.out.println("  [LongSender] Finished 3000ms upload without interruption.");
            } catch (InterruptedException e) {
                System.out.println("  [LongSender] Caught InterruptedException! Woken up prematurely by interrupt().");
                System.out.println("  [LongSender] Interrupted status was cleared by sleep(). Exiting safely.");
                return;
            }
        }, "Long-Running-Sender");

        longRunningSender.start();

        // Main thread waits 300ms, then issues interrupt
        try {
            System.out.println("Main thread waiting 300ms before sending interrupt signal...");
            Thread.sleep(300);
            System.out.println("Main thread calling longRunningSender.interrupt()...\n");
            longRunningSender.interrupt();

            // Wait for thread to clean up and exit
            longRunningSender.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted.");
        }

        System.out.println("\n[✓] Interruption demonstration complete.\n");
    }

    /**
     * Demo 5: Thread Lifecycle States.
     */
    public void demonstrateLifecycleStates() {
        ThreadLifecycleDemo.demonstrateLifecycleStates();
    }

    /**
     * Demo 6: Daemon vs Non-Daemon Thread Behavior.
     */
    public void demonstrateDaemonThread() {
        System.out.println("======================================================================");
        System.out.println("     6. Daemon vs Non-Daemon Thread Behavior                          ");
        System.out.println("======================================================================\n");

        MessageStatusMonitorDaemon daemonTask = new MessageStatusMonitorDaemon(300);
        Thread daemonThread = new Thread(daemonTask, "StatusMonitor-Daemon");

        System.out.println("Configuring thread as a DAEMON via setDaemon(true):");
        daemonThread.setDaemon(true); // Must be set BEFORE start()
        System.out.printf("Thread '%s' isDaemon: %b%n", daemonThread.getName(), daemonThread.isDaemon());

        daemonThread.start();

        // Let daemon run for a few pulses
        try {
            System.out.println("Main thread allowing daemon to pulse for 700ms...");
            Thread.sleep(700);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted.");
        }

        System.out.println("\nStopping daemon monitor cleanly...");
        daemonTask.stopMonitor();
        daemonThread.interrupt();

        try {
            daemonThread.join(500);
        } catch (InterruptedException e) {
            System.out.println("Wait interrupted.");
        }

        System.out.println("Academic Note: When all user (non-daemon) threads complete, the JVM halts");
        System.out.println("immediately and discards remaining daemon threads without running finally blocks.");
        System.out.println("\n[✓] Daemon thread demonstration complete.\n");
    }

    /**
     * Demo 7: Thread Names, IDs, and Priorities.
     */
    public void demonstrateThreadProperties() {
        System.out.println("======================================================================");
        System.out.println("     7. Thread Names, IDs, and Priorities                             ");
        System.out.println("======================================================================\n");

        Thread mainThread = Thread.currentThread();
        System.out.println("Inspecting Current (Main) Thread:");
        System.out.printf("  Name     : %s%n", mainThread.getName());
        System.out.printf("  ID       : %d%n", mainThread.threadId());
        System.out.printf("  Priority : %d (Default NORM_PRIORITY = %d)%n",
                mainThread.getPriority(), Thread.NORM_PRIORITY);
        System.out.printf("  State    : %s%n", mainThread.getState());
        System.out.printf("  isDaemon : %b%n%n", mainThread.isDaemon());

        System.out.println("Creating threads with different priorities (MIN_PRIORITY=1, MAX_PRIORITY=10):");

        Thread lowPriorityThread = new Thread(() -> {
            System.out.printf("  -> Low Priority Worker executing (Priority: %d)%n",
                    Thread.currentThread().getPriority());
        }, "LowPriorityWorker");
        lowPriorityThread.setPriority(Thread.MIN_PRIORITY);

        Thread highPriorityThread = new Thread(() -> {
            System.out.printf("  -> High Priority Worker executing (Priority: %d)%n",
                    Thread.currentThread().getPriority());
        }, "HighPriorityWorker");
        highPriorityThread.setPriority(Thread.MAX_PRIORITY);

        lowPriorityThread.start();
        highPriorityThread.start();

        try {
            lowPriorityThread.join();
            highPriorityThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted waiting for priority threads.");
        }

        System.out.println("\nAcademic Note: Thread priority is a HINT to the operating system.");
        System.out.println("Modern operating system schedulers (Windows, Linux, macOS) do NOT guarantee");
        System.out.println("that a thread with priority 10 will always execute before priority 1.");
        System.out.println("\n[✓] Thread properties demonstration complete.\n");
    }

    /**
     * Runs all syllabus demonstrations in sequence.
     */
    public void runAllDemonstrations() {
        demonstrateThreadVsRunnable();
        demonstrateStartVsRun();
        demonstrateSleepAndJoin();
        demonstrateInterruption();
        demonstrateLifecycleStates();
        demonstrateDaemonThread();
        demonstrateThreadProperties();
    }
}
