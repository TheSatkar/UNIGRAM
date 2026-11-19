package com.unigram.threading;

/**
 * Demonstrates creating a thread by EXTENDING the java.lang.Thread class.
 * 
 * Simulates a student user sending messages sequentially with delay intervals.
 * Demonstrates:
 * - Thread subclassing and overriding run()
 * - Thread naming and threadId()
 * - Thread.sleep() for simulating real-time typing/network transmission
 * - InterruptedException handling and graceful shutdown
 */
public class StudentMessageSenderThread extends Thread {

    private final String studentId;
    private final String conversationId;
    private final int messageCount;
    private final long delayMillis;

    public StudentMessageSenderThread(String studentId, String conversationId, int messageCount, long delayMillis) {
        // Set thread name using super()
        super("StudentSender-" + studentId);
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        this.studentId = studentId.trim();
        this.conversationId = conversationId;
        this.messageCount = messageCount;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        System.out.printf("[%s | ID: %d] Thread started execution on thread: %s%n",
                getName(), threadId(), Thread.currentThread().getName());

        for (int i = 1; i <= messageCount; i++) {
            // Check if thread was interrupted before proceeding
            if (isInterrupted()) {
                System.out.printf("[%s] Interrupt flag detected before message #%d. Halting.%n", getName(), i);
                return;
            }

            System.out.printf("[%s] Dispatched simulated message #%d/%d to conversation '%s'%n",
                    getName(), i, messageCount, conversationId);

            try {
                // Thread.sleep causes the thread to enter TIMED_WAITING state
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                System.out.printf("[%s] Interrupted while sleeping during message #%d! Cleaning up and exiting cleanly.%n",
                        getName(), i);
                // Standard Java practice: restore interrupted status or exit
                // Here we acknowledge the interrupt and exit the run() loop safely
                return;
            }
        }

        System.out.printf("[%s | ID: %d] Completed sending all %d messages. Thread finishing.%n",
                getName(), threadId(), messageCount);
    }

    public String getStudentId() {
        return studentId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
