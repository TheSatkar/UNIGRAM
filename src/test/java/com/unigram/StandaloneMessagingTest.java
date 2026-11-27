package com.unigram;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.User;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Targeted MySQL integration test for the standalone Uni-Gram application.
 * Uses a dedicated test schema (unigram_standalone_test_db) so demo data
 * in unigram_standalone_db is never touched.
 */
public class StandaloneMessagingTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
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
        System.out.println("       Uni-Gram Standalone Verification Test Suite                    ");
        System.out.println("======================================================================");

        DatabaseConfig baseConfig = new DatabaseConfig();
        if (!baseConfig.isConfigured()) {
            System.err.println("[!] Missing .env configuration. Cannot run MySQL verification.");
            System.exit(1);
        }

        String baseUrl = baseConfig.getUrl();
        String serverUrl = baseUrl.replace("unigram_standalone_db", "mysql");
        DatabaseConfig serverConfig = new DatabaseConfig(serverUrl, baseConfig.getUser(), baseConfig.getPassword());

        try (Connection conn = DatabaseConnectionFactory.getConnection(serverConfig);
             Statement st = conn.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS unigram_standalone_test_db");
            st.execute("CREATE DATABASE unigram_standalone_test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.execute("USE unigram_standalone_test_db");
            st.execute("CREATE TABLE users ("
                    + "user_id VARCHAR(64) NOT NULL, display_name VARCHAR(128) NOT NULL, "
                    + "email VARCHAR(128) NOT NULL, password_hash VARCHAR(255) NOT NULL DEFAULT '', "
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), "
                    + "PRIMARY KEY (user_id), UNIQUE KEY uq_user_email (email)) ENGINE=InnoDB");
            st.execute("CREATE TABLE conversations ("
                    + "conversation_id VARCHAR(64) NOT NULL, participant_one_id VARCHAR(64) NOT NULL, "
                    + "participant_two_id VARCHAR(64) NOT NULL, "
                    + "direct_pair_key VARCHAR(130) GENERATED ALWAYS AS (CONCAT(LEAST(LOWER(participant_one_id), LOWER(participant_two_id)), ':', GREATEST(LOWER(participant_one_id), LOWER(participant_two_id)))) VIRTUAL, "
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), "
                    + "PRIMARY KEY (conversation_id), UNIQUE KEY uq_conv_direct_pair (direct_pair_key), "
                    + "FOREIGN KEY (participant_one_id) REFERENCES users (user_id), "
                    + "FOREIGN KEY (participant_two_id) REFERENCES users (user_id)) ENGINE=InnoDB");
            st.execute("CREATE TABLE messages ("
                    + "message_id VARCHAR(64) NOT NULL, conversation_id VARCHAR(64) NOT NULL, "
                    + "sender_id VARCHAR(64) NOT NULL, content TEXT NOT NULL, "
                    + "sent_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), "
                    + "PRIMARY KEY (message_id), "
                    + "FOREIGN KEY (conversation_id) REFERENCES conversations (conversation_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (sender_id) REFERENCES users (user_id)) ENGINE=InnoDB");
        }

        String testUrl = baseUrl.replace("unigram_standalone_db", "unigram_standalone_test_db");
        DatabaseConfig testConfig = new DatabaseConfig(testUrl, baseConfig.getUser(), baseConfig.getPassword());

        try {
            JdbcChatService chat = new JdbcChatService(testConfig);
            JdbcMessageService msg = new JdbcMessageService(testConfig, chat);

            // 1. Student registration with passwords & duplicate rejection
            chat.registerUser("stu_1", "Alice", "alice@chitkara.edu.in", "pass111");
            chat.registerUser("stu_2", "Bob", "bob@chitkara.edu.in", "pass222");
            chat.registerUser("stu_3", "Charlie", "charlie@chitkara.edu.in", "pass333");
            check("Registered 3 students in MySQL", chat.getAllUsers().size() == 3);

            boolean dupIdRejected = false;
            try { chat.registerUser("stu_1", "Dup", "dup@chitkara.edu.in", "pass111"); } catch (UniGramException e) { dupIdRejected = true; }
            check("Duplicate student ID rejected", dupIdRejected);

            boolean dupEmailRejected = false;
            try { chat.registerUser("stu_4", "DupEmail", "alice@chitkara.edu.in", "pass444"); } catch (UniGramException e) { dupEmailRejected = true; }
            check("Duplicate email rejected", dupEmailRejected);

            // 2. Authentication checks
            User authed = chat.authenticate("stu_1", "pass111");
            check("Valid student login succeeds", authed != null && "stu_1".equals(authed.getUserId()));

            boolean wrongPassRejected = false;
            try { chat.authenticate("stu_1", "wrongpass"); } catch (UniGramException e) { wrongPassRejected = true; }
            check("Wrong password rejected", wrongPassRejected);

            boolean unknownUserRejected = false;
            try { chat.authenticate("unknown_99", "pass111"); } catch (UniGramException e) { unknownUserRejected = true; }
            check("Unknown student login rejected", unknownUserRejected);

            // 3. Direct conversation & reverse-direction deduplication
            Conversation conv = chat.startOrGetConversation("stu_1", "stu_2");
            Conversation sameConv = chat.startOrGetConversation("stu_1", "stu_2");
            Conversation reverseConv = chat.startOrGetConversation("stu_2", "stu_1");
            check("Direct conversation created with simple ID (conv_1)", conv != null && "conv_1".equals(conv.getConversationId()));
            check("Duplicate conversation returns same ID", conv.getConversationId().equals(sameConv.getConversationId()));
            check("Reverse-direction initiation returns same ID", conv.getConversationId().equals(reverseConv.getConversationId()));

            // 4. Invalid & self-conversation rejection
            boolean missingUserRejected = false;
            try { chat.startOrGetConversation("stu_1", "unknown_99"); } catch (UserNotFoundException e) { missingUserRejected = true; }
            check("Invalid student ID rejected", missingUserRejected);

            boolean selfRejected = false;
            try { chat.startOrGetConversation("stu_1", "stu_1"); } catch (IllegalArgumentException e) { selfRejected = true; }
            check("Self-conversation rejected", selfRejected);

            // 5. Conversations isolated to participants
            check("Participant 1 sees conversation", chat.getConversationsForUser("stu_1").size() == 1);
            check("Participant 2 sees conversation", chat.getConversationsForUser("stu_2").size() == 1);
            check("Non-participant Charlie sees 0 conversations", chat.getConversationsForUser("stu_3").isEmpty());

            // 6. Bidirectional messaging, empty message rejection, unauthorized access, chronological order
            msg.sendMessage(conv.getConversationId(), "stu_1", "Hello Bob!");
            Thread.sleep(10);
            msg.sendMessage(conv.getConversationId(), "stu_2", "Hi Alice!");

            boolean emptyRejected = false;
            try { msg.sendMessage(conv.getConversationId(), "stu_1", "   "); } catch (InvalidMessageException e) { emptyRejected = true; }
            check("Empty message rejected", emptyRejected);

            boolean unauthRejected = false;
            try { msg.getMessageHistory(conv.getConversationId(), "stu_3"); } catch (UnauthorizedConversationAccessException e) { unauthRejected = true; }
            check("Nonparticipant access rejected", unauthRejected);

            // 7. Restart persistence check using fresh service instances
            JdbcChatService freshChat = new JdbcChatService(testConfig);
            JdbcMessageService freshMsg = new JdbcMessageService(testConfig, freshChat);
            List<Message> history = freshMsg.getMessageHistory(conv.getConversationId(), "stu_1");
            check("Messages persist across restart in chronological order",
                    history.size() == 2
                            && "Hello Bob!".equals(history.get(0).getContent())
                            && "Hi Alice!".equals(history.get(1).getContent()));

            // 8. Concurrent conversation initiation safety
            ExecutorService pool = Executors.newFixedThreadPool(4);
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final boolean rev = (i % 2 == 1);
                tasks.add(() -> freshChat.startOrGetConversation(rev ? "stu_3" : "stu_1", rev ? "stu_1" : "stu_3").getConversationId());
            }
            List<Future<String>> futures = pool.invokeAll(tasks);
            pool.shutdown();
            String first = futures.get(0).get();
            boolean allSame = futures.stream().allMatch(f -> {
                try { return first.equals(f.get()); } catch (Exception e) { return false; }
            });
            check("Concurrent conversation initiation produces single thread", allSame);

            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("  Total Passed: " + passed + " | Total Failed: " + failed);
            System.out.println("----------------------------------------------------------------------");
            if (failed > 0) System.exit(1);
        } finally {
            try (Connection conn = DatabaseConnectionFactory.getConnection(serverConfig);
                 Statement st = conn.createStatement()) {
                st.execute("DROP DATABASE IF EXISTS unigram_standalone_test_db");
            }
        }
    }
}
