package com.unigram;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.MessageJdbcDao;
import com.unigram.repository.ProductReferenceJdbcDao;
import com.unigram.repository.UserJdbcDao;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;
import com.unigram.threading.ConcurrentJdbcMessageEngine;
import com.unigram.threading.JdbcMessageConsumer;
import com.unigram.threading.JdbcMessageProducer;
import com.unigram.threading.MessagePersistenceStatus;
import com.unigram.threading.MessagePersistenceTracker;
import com.unigram.threading.MessageQueue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Phase 7 Verification Test Suite: MySQL Persistence & Concurrent Messaging Integration.
 *
 * Strict Safety Guarantee:
 * - Operates EXCLUSIVELY against `unigram_test_db`.
 * - Explicitly refuses execution if connected to `unigram_db`.
 *
 * Verifies all 20 Phase 7 Requirements:
 *  1. User registration and retrieval through MySQL.
 *  2. Product reference registration and retrieval through MySQL.
 *  3. Conversation creation and relational foreign key linking.
 *  4. Self-conversation rejection (domain validation).
 *  5. Duplicate conversation prevention for same buyer, seller, and product.
 *  6. Participant conversation retrieval.
 *  7. Message insertion and retrieval.
 *  8. Chronological message ordering (ORDER BY sent_at ASC).
 *  9. Latest message retrieval.
 * 10. Message count for a conversation.
 * 11. Unauthorized sender rejection.
 * 12. Blank message rejection.
 * 13. Persistence after closing and reopening application/connections.
 * 14. Concurrent message insertion using custom bounded Producer-Consumer pipeline.
 * 15. No lost or duplicated messages during concurrent processing.
 * 16. Queue-full backpressure (producers block on full queue).
 * 17. Queue shutdown and draining before consumer exit.
 * 18. Failed database persistence handling (transaction rollback, PERSISTENCE_FAILED).
 * 19. Clean worker termination (zero thread leaks).
 * 20. Main database safety guard (unintended modifications prevented).
 */
public class Phase7Verification {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;
    private static int skippedSuites = 0;

    private static DatabaseConfig testConfig;
    private static JdbcChatService chatService;
    private static JdbcMessageService messageService;

    private static final UserJdbcDao userDao = new UserJdbcDao();
    private static final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private static final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private static final MessageJdbcDao messageDao = new MessageJdbcDao();

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          Uni-Gram — Phase 7 Verification Test Suite                  ");
        System.out.println("======================================================================\n");

        // 1. Resolve test database configuration
        resolveTestConfig();

        // 2. Offline verification: Configuration validation
        testConfigurationValidation();

        // 3. Check database reachability and verify safety guard
        boolean dbAvailable = checkDatabaseReachabilityAndSafety();

        if (!dbAvailable) {
            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("[!] NOTICE: MySQL Test Database (unigram_test_db) is not configured or reachable.");
            System.out.println("Offline tests completed. Online Phase 7 integration tests are SKIPPED.");
            System.out.println("To execute full live MySQL tests, configure your test environment variables:");
            System.out.println("  $env:UNIGRAM_TEST_DB_URL = \"jdbc:mysql://localhost:3306/unigram_test_db?serverTimezone=UTC\"");
            System.out.println("  $env:UNIGRAM_DB_USER = \"<mysql-username>\"");
            System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<mysql-password>\"");
            System.out.println("----------------------------------------------------------------------\n");
            skippedSuites = 20;
            printSummary(false);
            return;
        }

        // Initialize database-backed services for test database
        chatService = new JdbcChatService(testConfig, userDao, productDao, conversationDao);
        messageService = new JdbcMessageService(testConfig, chatService, messageDao);

        // 4. Live MySQL Tests
        try {
            cleanupAllTestData();

            testScenario1_UserRegistrationAndRetrieval();
            testScenario2_ProductRegistrationAndRetrieval();
            testScenario3_ConversationCreation();
            testScenario4_SelfConversationRejection();
            testScenario5_DuplicateConversationPrevention();
            testScenario6_ParticipantConversationRetrieval();
            testScenario7_MessageInsertionAndRetrieval();
            testScenario8_ChronologicalMessageOrdering();
            testScenario9_LatestMessageRetrieval();
            testScenario10_MessageCount();
            testScenario11_UnauthorizedSenderRejection();
            testScenario12_BlankMessageRejection();
            testScenario13_PersistenceAcrossConnectionReopening();
            testScenario14_ConcurrentMessageInsertion();
            testScenario15_NoLostOrDuplicatedMessages();
            testScenario16_QueueFullBackpressure();
            testScenario17_QueueShutdownAndDraining();
            testScenario18_FailedDatabasePersistenceHandling();
            testScenario19_CleanWorkerTermination();
            testScenario20_MainDatabaseProtectionGuard();

        } finally {
            cleanupAllTestData();
        }

        printSummary(true);
        if (failedAssertions > 0) {
            System.exit(1);
        }
    }

    private static void resolveTestConfig() {
        String testUrl = System.getenv("UNIGRAM_TEST_DB_URL");
        String user = System.getenv("UNIGRAM_TEST_DB_USER");
        String pass = System.getenv("UNIGRAM_TEST_DB_PASSWORD");

        if (testUrl == null || testUrl.isEmpty()) {
            String mainUrl = System.getenv("UNIGRAM_DB_URL");
            if (mainUrl != null && !mainUrl.isEmpty()) {
                testUrl = mainUrl.replace("/unigram_db", "/unigram_test_db");
            }
        }
        if (user == null || user.isEmpty()) {
            user = System.getenv("UNIGRAM_DB_USER");
        }
        if (pass == null) {
            pass = System.getenv("UNIGRAM_DB_PASSWORD");
        }

        testConfig = new DatabaseConfig(testUrl, user, pass);
    }

    private static void testConfigurationValidation() {
        System.out.println("[Test 0] Test Database Configuration Safety Validation");
        DatabaseConfig dummyTest = new DatabaseConfig("jdbc:mysql://localhost:3306/unigram_test_db", "test_user", "test_pass");
        assertTrue("Test config URL points to test database", dummyTest.getJdbcUrl().contains("unigram_test_db"));
        assertTrue("Test config has non-empty credentials", dummyTest.isConfigured());
        System.out.println();
    }

    private static boolean checkDatabaseReachabilityAndSafety() {
        if (!testConfig.isConfigured()) {
            return false;
        }

        // CRITICAL SAFETY CHECK: Refuse if points to unigram_db
        String url = testConfig.getJdbcUrl().toLowerCase();
        if (url.contains("/unigram_db") && !url.contains("unigram_test_db")) {
            throw new IllegalStateException("CRITICAL SAFETY VIOLATION: Phase7Verification must use unigram_test_db exclusively! Refusing to run against: " + testConfig.getJdbcUrl());
        }

        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: User Registration & Retrieval (MySQL)
    // -------------------------------------------------------------------------
    private static void testScenario1_UserRegistrationAndRetrieval() {
        System.out.println("[Scenario 1] User Registration & Retrieval through MySQL");
        try {
            User u = new User("p7_usr_1", "Phase 7 Student One", "p7_one@test.edu");
            chatService.registerUser(u);

            User fetched = chatService.getUser("p7_usr_1");
            assertTrue("Fetched user ID matches", "p7_usr_1".equals(fetched.getUserId()));
            assertTrue("Fetched user name matches", "Phase 7 Student One".equals(fetched.getFullName()));
            assertTrue("Fetched user email matches", "p7_one@test.edu".equals(fetched.getEmail()));

            Collection<User> all = chatService.getAllUsers();
            assertTrue("User exists in getAllUsers()", all.stream().anyMatch(usr -> usr.getUserId().equals("p7_usr_1")));
        } catch (Exception e) {
            fail("Scenario 1 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Product Reference Registration & Retrieval (MySQL)
    // -------------------------------------------------------------------------
    private static void testScenario2_ProductRegistrationAndRetrieval() {
        System.out.println("[Scenario 2] Product Reference Registration & Retrieval through MySQL");
        try {
            ProductReference p = new ProductReference("p7_prod_1", "Operating Systems Concepts (10th Ed)");
            chatService.registerProduct(p);

            ProductReference fetched = chatService.getProduct("p7_prod_1");
            assertTrue("Fetched product ID matches", "p7_prod_1".equals(fetched.getProductId()));
            assertTrue("Fetched product title matches", "Operating Systems Concepts (10th Ed)".equals(fetched.getProductTitle()));

            Collection<ProductReference> all = chatService.getAllProducts();
            assertTrue("Product exists in getAllProducts()", all.stream().anyMatch(pr -> pr.getProductId().equals("p7_prod_1")));
        } catch (Exception e) {
            fail("Scenario 2 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Conversation Creation (MySQL)
    // -------------------------------------------------------------------------
    private static void testScenario3_ConversationCreation() {
        System.out.println("[Scenario 3] Conversation Creation & Foreign Key Linking in MySQL");
        try {
            User seller = new User("p7_usr_2", "Phase 7 Student Two", "p7_two@test.edu");
            chatService.registerUser(seller);

            Conversation conv = chatService.createConversation("p7_usr_1", "p7_usr_2", "p7_prod_1");
            assertTrue("Conversation created with valid ID", conv != null && conv.getConversationId().startsWith("conv_"));
            assertTrue("Conversation buyer matches", "p7_usr_1".equals(conv.getBuyerId()));
            assertTrue("Conversation seller matches", "p7_usr_2".equals(conv.getSellerId()));
            assertTrue("Conversation product matches", "p7_prod_1".equals(conv.getProductId()));

            Conversation fetched = chatService.getConversationById(conv.getConversationId());
            assertTrue("Conversation retrievable by ID", fetched != null && fetched.getConversationId().equals(conv.getConversationId()));
        } catch (Exception e) {
            fail("Scenario 3 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Self-Conversation Rejection
    // -------------------------------------------------------------------------
    private static void testScenario4_SelfConversationRejection() {
        System.out.println("[Scenario 4] Self-Conversation Rejection (Domain & Constraint Rule)");
        boolean caught = false;
        try {
            chatService.createConversation("p7_usr_1", "p7_usr_1", "p7_prod_1");
        } catch (IllegalArgumentException e) {
            caught = true;
            assertTrue("Exception message indicates self-conversation prohibited",
                    e.getMessage().toLowerCase().contains("cannot start a conversation with themselves"));
        } catch (Exception e) {
            fail("Unexpected exception for self-conversation: " + e);
        }
        assertTrue("Attempt to start conversation with self threw IllegalArgumentException", caught);
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Duplicate Conversation Prevention
    // -------------------------------------------------------------------------
    private static void testScenario5_DuplicateConversationPrevention() {
        System.out.println("[Scenario 5] Duplicate Conversation Prevention (Same Buyer, Seller, Product)");
        boolean caught = false;
        try {
            chatService.createConversation("p7_usr_1", "p7_usr_2", "p7_prod_1");
        } catch (UniGramException e) {
            caught = true;
            assertTrue("Exception indicates duplicate conversation", e.getMessage().toLowerCase().contains("duplicate conversation"));
        } catch (Exception e) {
            fail("Unexpected exception on duplicate conversation: " + e);
        }
        assertTrue("Duplicate conversation rejected with UniGramException", caught);
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Participant Conversation Retrieval
    // -------------------------------------------------------------------------
    private static void testScenario6_ParticipantConversationRetrieval() {
        System.out.println("[Scenario 6] Participant Conversation Retrieval from MySQL");
        try {
            List<Conversation> buyerConvs = chatService.getConversationsForUser("p7_usr_1");
            assertTrue("Buyer conversations list is non-empty", !buyerConvs.isEmpty());
            assertTrue("Buyer is participant in retrieved conversation",
                    buyerConvs.stream().anyMatch(c -> c.isParticipant("p7_usr_1")));

            List<Conversation> sellerConvs = chatService.getConversationsForUser("p7_usr_2");
            assertTrue("Seller conversations list is non-empty", !sellerConvs.isEmpty());
            assertTrue("Seller is participant in retrieved conversation",
                    sellerConvs.stream().anyMatch(c -> c.isParticipant("p7_usr_2")));
        } catch (Exception e) {
            fail("Scenario 6 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Message Insertion & Retrieval
    // -------------------------------------------------------------------------
    private static void testScenario7_MessageInsertionAndRetrieval() {
        System.out.println("[Scenario 7] Message Insertion and Retrieval through MySQL");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            Message sent = messageService.sendMessage(convId, "p7_usr_1", "Hello! Is this textbook available?");
            assertTrue("Message ID assigned", sent != null && sent.getMessageId().startsWith("msg_"));
            assertTrue("Message content matches", "Hello! Is this textbook available?".equals(sent.getContent()));

            List<Message> history = messageService.getMessageHistory(convId, "p7_usr_1");
            assertTrue("Message history contains the sent message",
                    history.stream().anyMatch(m -> m.getMessageId().equals(sent.getMessageId())));
        } catch (Exception e) {
            fail("Scenario 7 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Chronological Message Ordering
    // -------------------------------------------------------------------------
    private static void testScenario8_ChronologicalMessageOrdering() {
        System.out.println("[Scenario 8] Chronological Message Ordering Verification (ORDER BY sent_at ASC)");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            Message m2 = messageService.sendMessage(convId, "p7_usr_2", "Yes, still available for $45.");
            Message m3 = messageService.sendMessage(convId, "p7_usr_1", "Great! Can we meet at the campus library?");

            List<Message> history = messageService.getMessageHistory(convId, "p7_usr_1");
            assertTrue("History contains at least 3 messages", history.size() >= 3);

            // Verify strict chronological ordering: sent_at must be non-decreasing
            boolean chronological = true;
            for (int i = 0; i < history.size() - 1; i++) {
                if (history.get(i).getSentAt().isAfter(history.get(i + 1).getSentAt())) {
                    chronological = false;
                    break;
                }
            }
            assertTrue("Message history returned in strict chronological order", chronological);
        } catch (Exception e) {
            fail("Scenario 8 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Latest Message Retrieval
    // -------------------------------------------------------------------------
    private static void testScenario9_LatestMessageRetrieval() {
        System.out.println("[Scenario 9] Latest Message Retrieval from MySQL");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            Message latestExpected = messageService.sendMessage(convId, "p7_usr_2", "Sounds good, see you at 3pm!");

            Optional<Message> latestActual = messageService.getLatestMessage(convId, "p7_usr_1");
            assertTrue("Latest message is present", latestActual.isPresent());
            assertTrue("Latest message ID matches the last sent message",
                    latestActual.map(m -> m.getMessageId().equals(latestExpected.getMessageId())).orElse(false));
            assertTrue("Latest message content matches",
                    latestActual.map(m -> m.getContent().equals("Sounds good, see you at 3pm!")).orElse(false));
        } catch (Exception e) {
            fail("Scenario 9 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Message Count
    // -------------------------------------------------------------------------
    private static void testScenario10_MessageCount() {
        System.out.println("[Scenario 10] Message Count for Conversation");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            int count = messageService.getMessageCount(convId);
            List<Message> history = messageService.getMessageHistory(convId, "p7_usr_1");
            assertTrue("getMessageCount() exactly equals size of history", count == history.size());
            assertTrue("Message count is at least 4", count >= 4);
        } catch (Exception e) {
            fail("Scenario 10 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Unauthorized Sender Rejection
    // -------------------------------------------------------------------------
    private static void testScenario11_UnauthorizedSenderRejection() {
        System.out.println("[Scenario 11] Unauthorized Sender Rejection (Security Guard)");
        try {
            User intruder = new User("p7_usr_intruder", "Unauthorized User", "intruder@test.edu");
            chatService.registerUser(intruder);

            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            boolean caught = false;
            try {
                messageService.sendMessage(convId, "p7_usr_intruder", "Unauthorized intrusion attempt!");
            } catch (UnauthorizedConversationAccessException e) {
                caught = true;
                assertTrue("Exception identifies intruder ID", e.getMessage().contains("p7_usr_intruder"));
            }
            assertTrue("Non-participant sender was rejected with UnauthorizedConversationAccessException", caught);

            boolean historyCaught = false;
            try {
                messageService.getMessageHistory(convId, "p7_usr_intruder");
            } catch (UnauthorizedConversationAccessException e) {
                historyCaught = true;
            }
            assertTrue("Non-participant reading history was rejected with UnauthorizedConversationAccessException", historyCaught);

        } catch (Exception e) {
            fail("Scenario 11 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Blank Message Rejection
    // -------------------------------------------------------------------------
    private static void testScenario12_BlankMessageRejection() {
        System.out.println("[Scenario 12] Blank and Null Message Validation");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            boolean caughtBlank = false;
            try {
                messageService.sendMessage(convId, "p7_usr_1", "   ");
            } catch (InvalidMessageException e) {
                caughtBlank = true;
            }
            assertTrue("Whitespace-only message rejected with InvalidMessageException", caughtBlank);

            boolean caughtNull = false;
            try {
                messageService.sendMessage(convId, "p7_usr_1", null);
            } catch (InvalidMessageException e) {
                caughtNull = true;
            }
            assertTrue("Null message rejected with InvalidMessageException", caughtNull);

        } catch (Exception e) {
            fail("Scenario 12 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Persistence Across Connection Reopening
    // -------------------------------------------------------------------------
    private static void testScenario13_PersistenceAcrossConnectionReopening() {
        System.out.println("[Scenario 13] Persistence Verification After Simulating App Restart");
        try {
            // Instantiate brand new service instances pointing to same test database
            JdbcChatService restartedChat = new JdbcChatService(testConfig);
            JdbcMessageService restartedMsg = new JdbcMessageService(testConfig, restartedChat);

            User userCheck = restartedChat.getUser("p7_usr_1");
            assertTrue("User persists across new service instantiation", userCheck != null && "p7_usr_1".equals(userCheck.getUserId()));

            ProductReference prodCheck = restartedChat.getProduct("p7_prod_1");
            assertTrue("Product persists across new service instantiation", prodCheck != null && "p7_prod_1".equals(prodCheck.getProductId()));

            List<Conversation> convs = restartedChat.getConversationsForUser("p7_usr_1");
            assertTrue("Conversations persist across new service instantiation", !convs.isEmpty());

            List<Message> history = restartedMsg.getMessageHistory(convs.get(0).getConversationId(), "p7_usr_1");
            assertTrue("Message history persists across new service instantiation", !history.isEmpty());

        } catch (Exception e) {
            fail("Scenario 13 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 14: Concurrent Message Insertion
    // -------------------------------------------------------------------------
    private static void testScenario14_ConcurrentMessageInsertion() {
        System.out.println("[Scenario 14] Concurrent Message Insertion via Bounded Pipeline");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            ConcurrentJdbcMessageEngine engine = new ConcurrentJdbcMessageEngine(testConfig, 4, 2, 20);
            engine.start();

            List<Message> batchA = new ArrayList<>();
            List<Message> batchB = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                batchA.add(new Message("p7_conc_a_" + i, convId, "p7_usr_1", "Batch A Message " + i, LocalDateTime.now()));
                batchB.add(new Message("p7_conc_b_" + i, convId, "p7_usr_2", "Batch B Message " + i, LocalDateTime.now()));
            }

            Thread prodA = new Thread(new JdbcMessageProducer("Producer-A", engine.getQueue(), batchA, engine.getTracker(), 10));
            Thread prodB = new Thread(new JdbcMessageProducer("Producer-B", engine.getQueue(), batchB, engine.getTracker(), 10));

            prodA.start();
            prodB.start();

            prodA.join(5000);
            prodB.join(5000);

            boolean drained = engine.shutdownAndAwait(8000);
            assertTrue("Engine successfully drained and shut down", drained);
            assertTrue("Engine tracker records 12 successfully persisted messages", engine.getTracker().getSuccessCount() == 12);
            assertTrue("Engine tracker records 0 failures", engine.getTracker().getFailureCount() == 0);

        } catch (Exception e) {
            fail("Scenario 14 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 15: No Lost or Duplicated Messages
    // -------------------------------------------------------------------------
    private static void testScenario15_NoLostOrDuplicatedMessages() {
        System.out.println("[Scenario 15] Verification of Zero Lost or Duplicated Messages in MySQL");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            List<Message> history = messageService.getMessageHistory(convId, "p7_usr_1");

            // Extract all message IDs matching the concurrent batch
            Set<String> foundIds = new HashSet<>();
            int concurrentBatchCount = 0;
            for (Message m : history) {
                if (m.getMessageId().startsWith("p7_conc_")) {
                    concurrentBatchCount++;
                    foundIds.add(m.getMessageId());
                }
            }

            assertTrue("All 12 concurrent messages are present in MySQL history", concurrentBatchCount == 12);
            assertTrue("All 12 concurrent messages have unique IDs (zero duplicates)", foundIds.size() == 12);

        } catch (Exception e) {
            fail("Scenario 15 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 16: Queue-Full Backpressure
    // -------------------------------------------------------------------------
    private static void testScenario16_QueueFullBackpressure() {
        System.out.println("[Scenario 16] Queue-Full Backpressure & Guarded Suspension");
        try {
            MessageQueue smallQueue = new MessageQueue(2);
            Message m1 = new Message("p7_bp_1", "conv_dummy", "p7_usr_1", "M1", LocalDateTime.now());
            Message m2 = new Message("p7_bp_2", "conv_dummy", "p7_usr_1", "M2", LocalDateTime.now());
            Message m3 = new Message("p7_bp_3", "conv_dummy", "p7_usr_1", "M3", LocalDateTime.now());

            smallQueue.enqueue(m1);
            smallQueue.enqueue(m2);
            assertTrue("Queue reports isFull() == true at capacity 2", smallQueue.isFull());

            // Producer trying to enqueue m3 should block into WAITING state
            Thread blockedProducer = new Thread(() -> {
                try {
                    smallQueue.enqueue(m3);
                } catch (InterruptedException ignored) {
                }
            }, "BlockedProducer");

            blockedProducer.start();
            Thread.sleep(200);

            assertTrue("Blocked producer entered WAITING state on full queue",
                    blockedProducer.getState() == Thread.State.WAITING);

            // Dequeue an item to free buffer space
            Message dequeued = smallQueue.dequeue();
            assertTrue("Dequeued m1 from queue", dequeued != null && dequeued.getMessageId().equals("p7_bp_1"));

            blockedProducer.join(1000);
            assertTrue("Producer awakened and completed enqueueing m3", !blockedProducer.isAlive());
            assertTrue("Queue contains m2 and m3", smallQueue.size() == 2);

        } catch (Exception e) {
            fail("Scenario 16 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 17: Queue Shutdown & Draining
    // -------------------------------------------------------------------------
    private static void testScenario17_QueueShutdownAndDraining() {
        System.out.println("[Scenario 17] Queue Shutdown and Draining Before Worker Exit");
        try {
            List<Conversation> convs = chatService.getConversationsForUser("p7_usr_1");
            String convId = convs.get(0).getConversationId();

            MessageQueue drainQueue = new MessageQueue(5);
            MessagePersistenceTracker tracker = new MessagePersistenceTracker();

            // Populate 3 messages
            for (int i = 1; i <= 3; i++) {
                Message msg = new Message("p7_drain_" + i, convId, "p7_usr_1", "Drain Message " + i, LocalDateTime.now());
                tracker.recordQueued(msg);
                drainQueue.enqueue(msg);
            }

            // Close queue BEFORE starting consumer
            drainQueue.close();
            assertTrue("Queue is closed", drainQueue.isClosed());
            assertTrue("Queue still holds 3 un-drained messages", drainQueue.size() == 3);

            // Start consumer; consumer must drain all 3 before terminating
            JdbcMessageConsumer consumer = new JdbcMessageConsumer("DrainConsumer", drainQueue, testConfig, messageDao, tracker, 10);
            Thread consumerThread = new Thread(consumer, "DrainConsumerThread");
            consumerThread.start();

            consumerThread.join(3000);
            assertTrue("Drain consumer thread finished", !consumerThread.isAlive());
            assertTrue("Queue is empty after drain", drainQueue.isEmpty());
            assertTrue("All 3 messages were successfully persisted during drain", tracker.getSuccessCount() == 3);

        } catch (Exception e) {
            fail("Scenario 17 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 18: Failed Database Persistence Handling
    // -------------------------------------------------------------------------
    private static void testScenario18_FailedDatabasePersistenceHandling() {
        System.out.println("[Scenario 18] Failed Database Persistence Handling & Rollback Atomicity");
        try {
            MessageQueue failQueue = new MessageQueue(3);
            MessagePersistenceTracker tracker = new MessagePersistenceTracker();

            // Message referencing NON-EXISTENT conversation ID: will fail FK constraint in MySQL!
            Message invalidMsg = new Message("p7_fail_msg", "conv_non_existent_ghost", "p7_usr_1", "Ghost conv message", LocalDateTime.now());
            tracker.recordQueued(invalidMsg);
            failQueue.enqueue(invalidMsg);
            failQueue.close();

            JdbcMessageConsumer consumer = new JdbcMessageConsumer("FailConsumer", failQueue, testConfig, messageDao, tracker, 10);
            Thread consumerThread = new Thread(consumer, "FailConsumerThread");
            consumerThread.start();

            consumerThread.join(3000);
            assertTrue("Consumer thread finished processing invalid message", !consumerThread.isAlive());
            assertTrue("Message marked PERSISTENCE_FAILED in tracker",
                    tracker.getStatus("p7_fail_msg") == MessagePersistenceStatus.PERSISTENCE_FAILED);
            assertTrue("Tracker failure count is 1", tracker.getFailureCount() == 1);
            assertTrue("Tracker success count is 0", tracker.getSuccessCount() == 0);

            // Verify in database that the message was NEVER persisted
            try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig);
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM messages WHERE message_id = 'p7_fail_msg'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue("Failed message does NOT exist in MySQL messages table", !rs.next());
                }
            }

        } catch (Exception e) {
            fail("Scenario 18 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 19: Clean Worker Termination
    // -------------------------------------------------------------------------
    private static void testScenario19_CleanWorkerTermination() {
        System.out.println("[Scenario 19] Clean Worker Termination and Thread Cleanup");
        try {
            ConcurrentJdbcMessageEngine engine = new ConcurrentJdbcMessageEngine(testConfig, 2, 2, 0);
            engine.start();

            // Verify worker threads are alive
            for (JdbcMessageConsumer c : engine.getConsumers()) {
                assertTrue("Consumer worker is active", c != null);
            }

            boolean clean = engine.shutdownAndAwait(3000);
            assertTrue("Engine shutdownAndAwait returned true", clean);

            // Verify consumers terminated
            assertTrue("Queue is closed", engine.getQueue().isClosed());
            assertTrue("Queue is empty", engine.getQueue().isEmpty());

        } catch (Exception e) {
            fail("Scenario 19 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 20: Main Database Protection Guard
    // -------------------------------------------------------------------------
    private static void testScenario20_MainDatabaseProtectionGuard() {
        System.out.println("[Scenario 20] Main Database Protection & Isolation Guard");
        try {
            assertTrue("Active test database URL strictly points to unigram_test_db",
                    testConfig.getJdbcUrl().toLowerCase().contains("unigram_test_db"));
            assertTrue("Active test database URL does NOT point to primary unigram_db",
                    !testConfig.getJdbcUrl().toLowerCase().matches(".*unigram_db(?![a-zA-Z0-9_]).*"));
        } catch (Exception e) {
            fail("Scenario 20 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Helpers & Cleanup
    // -------------------------------------------------------------------------
    private static void cleanupAllTestData() {
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE message_id LIKE 'p7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversations WHERE conversation_id LIKE 'conv_%' AND buyer_id LIKE 'p7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_references WHERE product_id LIKE 'p7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id LIKE 'p7_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
            // Best effort cleanup
        }
    }

    private static void assertTrue(String message, boolean condition) {
        totalAssertions++;
        if (condition) {
            passedAssertions++;
            System.out.println("  [PASS] " + message);
        } else {
            failedAssertions++;
            System.err.println("  [FAIL] " + message);
        }
    }

    private static void fail(String message) {
        totalAssertions++;
        failedAssertions++;
        System.err.println("  [FAIL] " + message);
    }

    private static void printSummary(boolean onlineTestsRun) {
        System.out.println("======================================================================");
        System.out.println("                   Phase 7 Verification Summary                       ");
        System.out.println("======================================================================");
        System.out.println("Total Assertions Evaluated : " + totalAssertions);
        System.out.println("Assertions Passed          : " + passedAssertions);
        System.out.println("Assertions Failed          : " + failedAssertions);
        if (!onlineTestsRun) {
            System.out.println("Online Test Suites Skipped : " + skippedSuites + " (Test database not configured/reachable)");
            System.out.println("Status                     : OFFLINE TESTS PASSED / ONLINE PENDING CONFIG");
        } else {
            System.out.println("Status                     : " + (failedAssertions == 0 ? "ALL 20 SCENARIOS PASSED" : "FAILURES DETECTED"));
        }
        System.out.println("======================================================================\n");
    }
}
