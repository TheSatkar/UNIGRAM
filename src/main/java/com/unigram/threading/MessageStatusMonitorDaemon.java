package com.unigram.threading;

/**
 * Demonstrates DAEMON THREAD behavior in Java.
 * 
 * Simulates a periodic background monitor checking messaging network status or pending unread counts.
 * Demonstrates:
 * - thread.setDaemon(true) before starting the thread
 * - JVM behavior: JVM automatically terminates when all non-daemon user threads finish,
 *   without waiting for daemon threads to finish.
 * - Graceful loop termination via interruption or a shutdown flag.
 */
public class MessageStatusMonitorDaemon implements Runnable {

    private final long intervalMillis;
    private volatile boolean running = true;

    public MessageStatusMonitorDaemon(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        Thread current = Thread.currentThread();
        System.out.printf("[DaemonMonitor] Started on thread '%s' (isDaemon: %b, Priority: %d)%n",
                current.getName(), current.isDaemon(), current.getPriority());

        int pulse = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            pulse++;
            System.out.printf("[DaemonMonitor - Pulse #%d] Status: In-memory messaging channels healthy.%n", pulse);

            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                System.out.println("[DaemonMonitor] Interrupted during sleep. Exiting daemon loop gracefully.");
                break;
            }
        }

        System.out.println("[DaemonMonitor] Shutdown complete.");
    }

    public void stopMonitor() {
        this.running = false;
    }
}
