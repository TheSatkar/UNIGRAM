package com.unigram.threading;

/**
 * Demonstrates creating a thread by IMPLEMENTING the java.lang.Runnable interface.
 * 
 * Demonstrates:
 * - Decoupling task logic from thread execution (favor composition over inheritance)
 * - Thread.currentThread() to inspect the thread currently executing this Runnable
 * - Thread naming and thread priority inspection
 * - Simulating an independent student activity (browsing marketplace listings)
 */
public class StudentListingBrowserTask implements Runnable {

    private final String studentId;
    private final String category;
    private final int itemsToBrowse;
    private final long delayMillis;

    public StudentListingBrowserTask(String studentId, String category, int itemsToBrowse, long delayMillis) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        this.studentId = studentId.trim();
        this.category = category != null ? category : "General";
        this.itemsToBrowse = itemsToBrowse;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        Thread current = Thread.currentThread();
        System.out.printf("[RunnableTask for %s] Running on thread '%s' (ID: %d, Priority: %d)%n",
                studentId, current.getName(), current.threadId(), current.getPriority());

        for (int i = 1; i <= itemsToBrowse; i++) {
            if (Thread.interrupted()) {
                System.out.printf("[RunnableTask for %s] Interrupted! Stopping listing browse at item #%d.%n",
                        studentId, i);
                return;
            }

            System.out.printf("[RunnableTask for %s] Browsing item #%d in category '%s'%n",
                    studentId, i, category);

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                System.out.printf("[RunnableTask for %s] Sleep interrupted during item #%d. Halting gracefully.%n",
                        studentId, i);
                return;
            }
        }

        System.out.printf("[RunnableTask for %s] Finished browsing %d listings.%n", studentId, itemsToBrowse);
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCategory() {
        return category;
    }
}
