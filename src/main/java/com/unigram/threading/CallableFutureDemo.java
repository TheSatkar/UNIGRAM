package com.unigram.threading;

import com.unigram.exception.InvalidMessageException;
import com.unigram.model.Message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates Callable and Future fundamentals in the Uni-Gram domain:
 * 1. Submitting Callable<T> and retrieving return value via Future.get()
 * 2. Blocking behavior of Future.get()
 * 3. Polling task status using Future.isDone()
 * 4. Bounded timeout retrieval using Future.get(timeout, unit) and TimeoutException
 * 5. Exception propagation from background tasks via ExecutionException and getCause()
 */
public class CallableFutureDemo {

    /**
     * Callable Task 1: Counts messages matching a specific conversation ID.
     */
    public static class MessageCounterTask implements Callable<Integer> {
        private final String conversationId;
        private final List<Message> messageList;

        public MessageCounterTask(String conversationId, List<Message> messageList) {
            this.conversationId = conversationId;
            this.messageList = messageList;
        }

        @Override
        public Integer call() throws Exception {
            Thread.sleep(150); // Simulate background processing delay
            int count = 0;
            for (Message m : messageList) {
                if (m.getConversationId().equalsIgnoreCase(conversationId)) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Callable Task 2: Calculates unread messages for a recipient.
     */
    public static class UnreadCalculatorTask implements Callable<Integer> {
        private final String recipientUserId;
        private final List<Message> messageList;

        public UnreadCalculatorTask(String recipientUserId, List<Message> messageList) {
            this.recipientUserId = recipientUserId;
            this.messageList = messageList;
        }

        @Override
        public Integer call() throws Exception {
            Thread.sleep(150);
            int unread = 0;
            for (Message m : messageList) {
                // In demo, messages sent by others to this recipient count as pending unread
                if (!m.getSenderId().equalsIgnoreCase(recipientUserId)) {
                    unread++;
                }
            }
            return unread;
        }
    }

    /**
     * Callable Task 3: Generates conversation audit summary.
     */
    public static class ConversationReportTask implements Callable<String> {
        private final String conversationId;

        public ConversationReportTask(String conversationId) {
            this.conversationId = conversationId;
        }

        @Override
        public String call() throws Exception {
            Thread.sleep(200);
            return String.format("Report for %s: Status=Active, Health=Optimal, Timestamp=%s",
                    conversationId, LocalDateTime.now());
        }
    }

    /**
     * Callable Task 4: Simulates a task that throws a checked domain exception.
     */
    public static class FailingValidationTask implements Callable<Boolean> {
        private final String invalidPayload;

        public FailingValidationTask(String invalidPayload) {
            this.invalidPayload = invalidPayload;
        }

        @Override
        public Boolean call() throws Exception {
            Thread.sleep(100);
            if (invalidPayload == null || invalidPayload.trim().isEmpty()) {
                throw new InvalidMessageException("Validation failed: Message content cannot be blank.");
            }
            return true;
        }
    }

    /**
     * Demonstrates successful Callable execution, Future.isDone(), and blocking Future.get().
     */
    public static void demonstrateCallableAndFuture() {
        System.out.println("======================================================================");
        System.out.println("           Callable and Future Result Demonstration                   ");
        System.out.println("======================================================================\n");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Message> sampleMessages = createSampleDataset();

        try {
            System.out.println("Submitting MessageCounterTask for conversation 'conv_101'...");
            Future<Integer> countFuture = executor.submit(new MessageCounterTask("conv_101", sampleMessages));

            System.out.println("Submitting UnreadCalculatorTask for student 'usr_maya'...");
            Future<Integer> unreadFuture = executor.submit(new UnreadCalculatorTask("usr_maya", sampleMessages));

            System.out.println("Submitting ConversationReportTask for 'conv_101'...");
            Future<String> reportFuture = executor.submit(new ConversationReportTask("conv_101"));

            // Check isDone() immediately (likely false as tasks are still sleeping/computing)
            System.out.printf("Immediate status: countFuture.isDone() = %b (tasks running asynchronously)%n%n",
                    countFuture.isDone());

            System.out.println("Calling Future.get() (This thread will block until computation completes)...");
            long startTime = System.currentTimeMillis();

            Integer messageCount = countFuture.get(2, TimeUnit.SECONDS);
            Integer unreadCount = unreadFuture.get(2, TimeUnit.SECONDS);
            String reportSummary = reportFuture.get(2, TimeUnit.SECONDS);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("All results retrieved in %d ms:%n", elapsed);
            System.out.println("  1. Total Messages in conv_101 : " + messageCount);
            System.out.println("  2. Unread Messages for Maya   : " + unreadCount);
            System.out.println("  3. Conversation Report        : " + reportSummary);
            System.out.printf("Post-computation status: countFuture.isDone() = %b%n", countFuture.isDone());

        } catch (InterruptedException e) {
            System.out.println("Demo interrupted.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.out.println("Task execution failed: " + e.getMessage());
        } catch (TimeoutException e) {
            System.out.println("Task timed out: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
        System.out.println();
    }

    /**
     * Demonstrates exception handling with Future: ExecutionException and unwrapping getCause().
     */
    public static void demonstrateFutureExceptions() {
        System.out.println("======================================================================");
        System.out.println("           Future Exception Handling (ExecutionException)            ");
        System.out.println("======================================================================\n");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            System.out.println("Submitting FailingValidationTask with blank content (throws InvalidMessageException)...");
            Future<Boolean> failingFuture = executor.submit(new FailingValidationTask("   "));

            System.out.println("Calling failingFuture.get()...");
            failingFuture.get(); // Will throw ExecutionException

        } catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.out.println("\n[✓] Caught Expected ExecutionException!");
            System.out.println("  Wrapper Exception Class: " + e.getClass().getName());
            System.out.println("  Wrapper Message        : " + e.getMessage());

            // Extract the underlying checked exception thrown by the Callable
            Throwable cause = e.getCause();
            System.out.println("  Unwrapped Cause Class  : " + cause.getClass().getName());
            System.out.println("  Original Cause Message : " + cause.getMessage());

            if (cause instanceof InvalidMessageException) {
                System.out.println("  -> Successfully identified original checked exception as InvalidMessageException.");
            }

            System.out.println("\n>> Academic Viva Note: ExecutionException <<");
            System.out.println("Exceptions thrown inside a Callable's call() method do not crash worker threads.");
            System.out.println("Instead, the ExecutorService captures the exception and re-throws it");
            System.out.println("wrapped in an ExecutionException when the caller invokes future.get().");

        } finally {
            executor.shutdown();
        }
        System.out.println();
    }

    private static List<Message> createSampleDataset() {
        List<Message> list = new ArrayList<>();
        list.add(new Message("msg_1", "conv_101", "usr_alex", "Hi Maya, textbook available?", LocalDateTime.now()));
        list.add(new Message("msg_2", "conv_101", "usr_maya", "Yes Alex, it is available.", LocalDateTime.now()));
        list.add(new Message("msg_3", "conv_101", "usr_alex", "Great, meeting at library?", LocalDateTime.now()));
        list.add(new Message("msg_4", "conv_102", "usr_alex", "Inquiry about calculator", LocalDateTime.now()));
        return list;
    }
}
