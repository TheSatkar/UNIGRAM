package com.unigram;

import com.unigram.collection.InMemoryStorage;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.service.ChatService;
import com.unigram.service.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Targeted verification suite for standalone direct student-to-student messaging
 * and compatibility with product-based conversations.
 */
public class DirectMessagingVerificationTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.err.println("  [FAIL] " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================================");
        System.out.println("     Uni-Gram Standalone Direct Messaging Verification Suite          ");
        System.out.println("======================================================================\n");

        InMemoryStorage storage = new InMemoryStorage();
        ChatService chatService = new ChatService(storage);
        MessageService messageService = new MessageService(storage, chatService);

        User alice = new User("stu_alice", "Alice Sharma", "alice@chitkara.edu.in");
        User bob = new User("stu_bob", "Bob Verma", "bob@chitkara.edu.in");
        User charlie = new User("stu_charlie", "Charlie Gill", "charlie@chitkara.edu.in");
        chatService.registerUser(alice);
        chatService.registerUser(bob);
        chatService.registerUser(charlie);

        // 1. Start direct conversation without product ID
        Conversation c1 = chatService.startDirectConversation("stu_alice", "stu_bob");
        assertTrue("Direct conversation created without product ID",
                c1 != null && c1.isDirectConversation() && c1.getProductId() == null);

        // 2. Duplicate prevention in both directions
        Conversation c1Same = chatService.startDirectConversation("stu_alice", "stu_bob");
        Conversation c1Reverse = chatService.startDirectConversation("stu_bob", "stu_alice");
        assertTrue("Same-direction lookup returns existing conversation",
                c1.getConversationId().equals(c1Same.getConversationId()));
        assertTrue("Reverse-direction lookup (B->A) returns identical conversation",
                c1.getConversationId().equals(c1Reverse.getConversationId()));

        // 3. Reject nonexistent student ID
        boolean missingRejected = false;
        try {
            chatService.startDirectConversation("stu_alice", "stu_nonexistent");
        } catch (UserNotFoundException e) {
            missingRejected = true;
        }
        assertTrue("Nonexistent student ID rejected with UserNotFoundException", missingRejected);

        // 4. Reject self-conversation
        boolean selfRejected = false;
        try {
            chatService.startDirectConversation("stu_alice", "stu_alice");
        } catch (IllegalArgumentException e) {
            selfRejected = true;
        }
        assertTrue("Self-conversation rejected with IllegalArgumentException", selfRejected);

        // 5. Bidirectional messaging and chronological history
        messageService.sendMessage(c1.getConversationId(), "stu_alice", "Hi Bob, are you free for the Java lab?");
        messageService.sendMessage(c1.getConversationId(), "stu_bob", "Yes Alice! Meet at Turing Block at 2 PM.");
        List<Message> history = messageService.getMessageHistory(c1.getConversationId(), "stu_alice");
        assertTrue("Chronological message history contains both messages in order",
                history.size() == 2
                        && "stu_alice".equals(history.get(0).getSenderId())
                        && "stu_bob".equals(history.get(1).getSenderId()));

        // 6. Reject unauthorized participant access
        boolean unauthorizedRejected = false;
        try {
            messageService.getMessageHistory(c1.getConversationId(), "stu_charlie");
        } catch (UnauthorizedConversationAccessException e) {
            unauthorizedRejected = true;
        }
        assertTrue("Non-participant rejected with UnauthorizedConversationAccessException", unauthorizedRejected);

        // 7. Concurrent direct conversation initiation does not create duplicates
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final boolean reverse = (i % 2 == 1);
            tasks.add(() -> chatService.startDirectConversation(
                    reverse ? "stu_charlie" : "stu_alice",
                    reverse ? "stu_alice" : "stu_charlie"
            ).getConversationId());
        }
        List<Future<String>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        String firstId = futures.get(0).get();
        boolean allSame = true;
        for (Future<String> f : futures) {
            if (!firstId.equals(f.get())) {
                allSame = false;
                break;
            }
        }
        assertTrue("Concurrent direct conversation initiation produces single thread", allSame);

        // 8. Existing product-based conversation remains supported alongside direct threads
        chatService.registerProduct(new ProductReference("prod_dsa_01", "DSA Textbook"));
        Conversation prodConv = chatService.createConversation("stu_alice", "stu_bob", "prod_dsa_01");
        assertTrue("Product-based conversation still supported separately from direct conversation",
                !prodConv.isDirectConversation()
                        && "prod_dsa_01".equals(prodConv.getProductId())
                        && !prodConv.getConversationId().equals(c1.getConversationId()));

        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("  Total Passed: " + passed + " | Total Failed: " + failed);
        System.out.println("----------------------------------------------------------------------");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
