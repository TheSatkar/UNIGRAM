package com.unigram;

import com.unigram.collection.InMemoryStorage;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.service.ChatService;
import com.unigram.service.MessageService;

import java.util.List;
import java.util.Optional;

/**
 * Direct standalone test verification for Phase 2 requirements.
 * Validates all business rules, collection operations, exception scenarios, and ordering.
 */
public class Phase2Verification {

    public static void main(String[] args) {
        System.out.println(">>> Running Phase 2 Automated Verification Suite <<<\n");
        int testsPassed = 0;

        InMemoryStorage storage = new InMemoryStorage();
        ChatService chatService = new ChatService(storage);
        MessageService messageService = new MessageService(storage, chatService);

        try {
            // 1. User registration & retrieval
            User u1 = new User("usr_test_1", "Test User One", "u1@test.edu");
            chatService.registerUser(u1);
            User fetchedU1 = chatService.getUser("usr_test_1");
            assertTrue(fetchedU1.equals(u1), "User registration and retrieval");
            testsPassed++;

            // 2. Product registration & retrieval
            ProductReference p1 = new ProductReference("prod_test_1", "Test Textbook");
            chatService.registerProduct(p1);
            ProductReference fetchedP1 = chatService.getProduct("prod_test_1");
            assertTrue(fetchedP1.equals(p1), "Product registration and retrieval");
            testsPassed++;

            // 3. Conversation creation
            User u2 = new User("usr_test_2", "Test User Two", "u2@test.edu");
            chatService.registerUser(u2);
            Conversation c1 = chatService.createConversation(u1.getUserId(), u2.getUserId(), p1.getProductId());
            assertTrue(c1 != null && c1.getConversationId() != null, "Conversation creation");
            testsPassed++;

            // 4. Duplicate conversation prevention
            boolean caughtDuplicate = false;
            try {
                chatService.createConversation(u1.getUserId(), u2.getUserId(), p1.getProductId());
            } catch (UniGramException e) {
                caughtDuplicate = true;
            }
            assertTrue(caughtDuplicate, "Duplicate conversation prevention");
            testsPassed++;

            // 5. Separate conversation for different product between same users
            ProductReference p2 = new ProductReference("prod_test_2", "Test Calculator");
            chatService.registerProduct(p2);
            Conversation c2 = chatService.createConversation(u1.getUserId(), u2.getUserId(), p2.getProductId());
            assertTrue(!c2.getConversationId().equals(c1.getConversationId()), "Separate conversations for different products");
            testsPassed++;

            // 6. Sending and retrieving messages
            Message m1 = messageService.sendMessage(c1.getConversationId(), u1.getUserId(), "First test message");
            Message m2 = messageService.sendMessage(c1.getConversationId(), u2.getUserId(), "Second test reply");
            List<Message> history = messageService.getMessageHistory(c1.getConversationId(), u1.getUserId());
            assertTrue(history.size() == 2, "Message sending and retrieval count");
            testsPassed++;

            // 7. Correct chronological order
            assertTrue(history.get(0).getMessageId().equals(m1.getMessageId()) &&
                       history.get(1).getMessageId().equals(m2.getMessageId()), "Correct chronological order");
            testsPassed++;

            // 8. Latest message retrieval
            Optional<Message> latest = messageService.getLatestMessage(c1.getConversationId(), u1.getUserId());
            assertTrue(latest.isPresent() && latest.get().getMessageId().equals(m2.getMessageId()), "Latest message retrieval");
            testsPassed++;

            // 9. Unauthorized access prevention
            boolean caughtUnauthorized = false;
            try {
                messageService.sendMessage(c1.getConversationId(), "usr_stranger", "Unauthorized message");
            } catch (UnauthorizedConversationAccessException e) {
                caughtUnauthorized = true;
            }
            assertTrue(caughtUnauthorized, "Unauthorized message sending rejection");
            testsPassed++;

            // 10. Blank message validation
            boolean caughtBlankMessage = false;
            try {
                messageService.sendMessage(c1.getConversationId(), u1.getUserId(), "    ");
            } catch (InvalidMessageException e) {
                caughtBlankMessage = true;
            }
            assertTrue(caughtBlankMessage, "Blank message content validation");
            testsPassed++;

            // 11. Missing user exception
            boolean caughtMissingUser = false;
            try {
                chatService.getUser("usr_non_existent");
            } catch (UserNotFoundException e) {
                caughtMissingUser = true;
            }
            assertTrue(caughtMissingUser, "Missing user exception");
            testsPassed++;

            // 12. Missing conversation exception
            boolean caughtMissingConv = false;
            try {
                chatService.getConversationById("conv_non_existent");
            } catch (ConversationNotFoundException e) {
                caughtMissingConv = true;
            }
            assertTrue(caughtMissingConv, "Missing conversation exception");
            testsPassed++;

            System.out.println("\n>>> All " + testsPassed + " Automated Verifications PASSED Successfully! <<<");

        } catch (Exception e) {
            System.err.println("TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (!condition) {
            System.err.println("FAILED: " + testName);
            throw new AssertionError("Test assertion failed: " + testName);
        } else {
            System.out.println("  [PASS] " + testName);
        }
    }
}
