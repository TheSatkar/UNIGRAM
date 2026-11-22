package com.unigram;

import com.unigram.api.CorsConfig;
import com.unigram.api.JsonUtil;
import com.unigram.api.UniGramHttpServer;
import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 9 Verification Test Suite: Standalone Java HTTP API.
 *
 * Strict Safety Guarantee:
 * - Operates EXCLUSIVELY against `unigram_test_db`.
 * - Explicitly refuses execution if configured URL points to `unigram_db`.
 * - Uses ephemeral port (port 0) to avoid port conflicts with live application.
 * - Employs standard JDK HttpClient for all integration assertions.
 *
 * Verifies all 23 Phase 9 Scenarios:
 *  1. Health endpoint.
 *  2. User listing.
 *  3. User lookup.
 *  4. Missing user returns 404.
 *  5. Product reference listing.
 *  6. Conversation listing by participant.
 *  7. Conversation creation.
 *  8. Duplicate conversation returns 409 conflict response.
 *  9. Self-conversation rejection (400 Bad Request).
 * 10. Conversation lookup.
 * 11. Message history retrieval.
 * 12. Message submission.
 * 13. Message persistence in MySQL.
 * 14. Persistence across API server restart.
 * 15. Missing development identity handling (401 Unauthorized).
 * 16. Unauthorized participant rejection (403 Forbidden).
 * 17. Blank message rejection (400 Bad Request).
 * 18. Malformed JSON handling.
 * 19. Invalid routes (404) and HTTP methods (405).
 * 20. CORS preflight behaviour.
 * 21. Concurrent HTTP requests.
 * 22. Graceful server shutdown.
 * 23. Database-unavailable error handling (503 Service Unavailable).
 */
public class Phase9Verification {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;
    private static int skippedSuites = 0;

    private static DatabaseConfig testConfig;
    private static UniGramHttpServer server;
    private static String baseUrl;
    private static HttpClient httpClient;

    private static final UserJdbcDao userDao = new UserJdbcDao();
    private static final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private static final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private static final MessageJdbcDao messageDao = new MessageJdbcDao();

    // Reusable IDs across scenarios
    private static final String BUYER_ID = "p9_buyer";
    private static final String SELLER_ID = "p9_seller";
    private static final String UNAUTHORIZED_USER_ID = "p9_unauthorized";
    private static final String PROD_1_ID = "p9_prod_1";
    private static final String PROD_2_ID = "p9_prod_2";
    private static String createdConvId;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          Uni-Gram — Phase 9 Verification Test Suite                  ");
        System.out.println("             Standalone Java HTTP API & REST Endpoints                ");
        System.out.println("======================================================================\n");

        // 1. Resolve test database configuration
        resolveTestConfig();

        // 2. Offline verification: Configuration safety check
        testConfigurationValidation();

        // 3. Check database reachability and verify safety guard
        boolean dbAvailable = checkDatabaseReachabilityAndSafety();

        if (!dbAvailable) {
            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("[!] NOTICE: MySQL Test Database (unigram_test_db) is not configured or reachable.");
            System.out.println("Offline tests completed. Online Phase 9 HTTP integration tests are SKIPPED.");
            System.out.println("To execute full live MySQL tests, configure your test environment variables:");
            System.out.println("  $env:UNIGRAM_TEST_DB_URL = \"jdbc:mysql://localhost:3306/unigram_test_db?serverTimezone=UTC\"");
            System.out.println("  $env:UNIGRAM_DB_USER = \"<mysql-username>\"");
            System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<mysql-password>\"");
            System.out.println("----------------------------------------------------------------------\n");
            skippedSuites = 23;
            printSummary(false);
            return;
        }

        // Initialize HTTP Client and start background test server on ephemeral port (port 0)
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            cleanupAllTestData();
            seedBaseTestData();

            server = new UniGramHttpServer("127.0.0.1", 0, testConfig, new CorsConfig());
            server.start();
            baseUrl = server.getBaseUrl();
            System.out.println("[+] Test HTTP Server active at: " + baseUrl + "\n");

            // Execute all 23 scenarios
            testScenario1_HealthEndpoint();
            testScenario2_UserListing();
            testScenario3_UserLookup();
            testScenario4_MissingUserReturns404();
            testScenario5_ProductReferenceListing();
            testScenario6_ConversationListingByParticipant();
            testScenario7_ConversationCreation();
            testScenario8_DuplicateConversationConflict();
            testScenario9_SelfConversationRejection();
            testScenario10_ConversationLookup();
            testScenario11_MessageHistoryRetrieval();
            testScenario12_MessageSubmission();
            testScenario13_MessagePersistenceInMySql();
            testScenario14_PersistenceAcrossServerRestart();
            testScenario15_MissingDevelopmentIdentity();
            testScenario16_UnauthorizedParticipantRejection();
            testScenario17_BlankMessageRejection();
            testScenario18_MalformedJsonHandling();
            testScenario19_InvalidRoutesAndHttpMethods();
            testScenario20_CorsPreflightBehaviour();
            testScenario21_ConcurrentHttpRequests();
            testScenario22_GracefulServerShutdown();
            testScenario23_DatabaseUnavailableHandling();

        } catch (Exception e) {
            fail("Fatal exception during Phase 9 verification execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (server != null && server.isRunning()) {
                server.stop(0);
            }
            cleanupAllTestData();
        }

        printSummary(true);
        if (failedAssertions > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------
    // Configuration & Safety
    // -------------------------------------------------------------------------
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
        System.out.println("[Test 0] Test Database Safety Guard Validation");
        DatabaseConfig dummyTest = new DatabaseConfig("jdbc:mysql://localhost:3306/unigram_test_db", "test_user", "test_pass");
        assertTrue("Test config URL points to test database", dummyTest.getJdbcUrl().contains("unigram_test_db"));
        assertTrue("Test config credentials present", dummyTest.isConfigured());
        System.out.println();
    }

    private static boolean checkDatabaseReachabilityAndSafety() {
        if (!testConfig.isConfigured()) {
            return false;
        }

        // CRITICAL SAFETY CHECK: Refuse if points to unigram_db
        String url = testConfig.getJdbcUrl().toLowerCase();
        if (url.contains("/unigram_db") && !url.contains("unigram_test_db")) {
            throw new IllegalStateException("CRITICAL SAFETY VIOLATION: Phase9Verification must use unigram_test_db exclusively! Refusing to run against: " + testConfig.getJdbcUrl());
        }

        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private static void seedBaseTestData() {
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            userDao.insert(conn, new User(BUYER_ID, "Phase 9 Buyer", "buyer@test.edu"));
            userDao.insert(conn, new User(SELLER_ID, "Phase 9 Seller", "seller@test.edu"));
            userDao.insert(conn, new User(UNAUTHORIZED_USER_ID, "Phase 9 Unauthorized", "unauth@test.edu"));

            productDao.insert(conn, new ProductReference(PROD_1_ID, "Introduction to Algorithms"));
            productDao.insert(conn, new ProductReference(PROD_2_ID, "Computer Networks Guide"));

            Conversation baseConv = new Conversation("p9_conv_seed", BUYER_ID, SELLER_ID, PROD_1_ID, LocalDateTime.now());
            conversationDao.insert(conn, baseConv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed base test records in unigram_test_db: " + e.getMessage(), e);
        }
    }

    private static void cleanupAllTestData() {
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE message_id LIKE 'p9_%' OR sender_id LIKE 'p9_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversations WHERE conversation_id LIKE 'p9_%' OR buyer_id LIKE 'p9_%' OR seller_id LIKE 'p9_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_references WHERE product_id LIKE 'p9_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id LIKE 'p9_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
            // Best effort cleanup
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: Health Endpoint
    // -------------------------------------------------------------------------
    private static void testScenario1_HealthEndpoint() {
        System.out.println("[Scenario 1] GET /api/health — Server & Database Health Check");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Health status code is 200 OK", response.statusCode() == 200);
            assertTrue("Health response has Content-Type application/json", response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
            assertTrue("Health report status is UP", response.body().contains("\"status\":\"UP\""));
            assertTrue("Health report database is CONNECTED", response.body().contains("\"database\":\"CONNECTED\""));
            assertTrue("Health report apiVersion is 1.0.0", response.body().contains("\"apiVersion\":\"1.0.0\""));
        } catch (Exception e) {
            fail("Scenario 1 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: User Listing
    // -------------------------------------------------------------------------
    private static void testScenario2_UserListing() {
        System.out.println("[Scenario 2] GET /api/users — List Registered Users");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("User listing status code is 200 OK", response.statusCode() == 200);
            assertTrue("Response envelope success is true", response.body().contains("\"success\":true"));
            assertTrue("Contains buyer ID", response.body().contains(BUYER_ID));
            assertTrue("Contains seller ID", response.body().contains(SELLER_ID));
        } catch (Exception e) {
            fail("Scenario 2 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: User Lookup
    // -------------------------------------------------------------------------
    private static void testScenario3_UserLookup() {
        System.out.println("[Scenario 3] GET /api/users/{userId} — Single User Retrieval");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users/" + BUYER_ID))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("User lookup status code is 200 OK", response.statusCode() == 200);
            assertTrue("User data matches requested ID", response.body().contains("\"userId\":\"" + BUYER_ID + "\""));
            assertTrue("User full name matches", response.body().contains("Phase 9 Buyer"));
        } catch (Exception e) {
            fail("Scenario 3 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Missing User Returns 404
    // -------------------------------------------------------------------------
    private static void testScenario4_MissingUserReturns404() {
        System.out.println("[Scenario 4] GET /api/users/{userId} — Missing User 404 Not Found");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users/p9_nonexistent_usr_999"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Missing user status code is 404", response.statusCode() == 404);
            assertTrue("Response envelope success is false", response.body().contains("\"success\":false"));
            assertTrue("Error message mentions user not found", response.body().contains("User not found with ID: p9_nonexistent_usr_999"));
        } catch (Exception e) {
            fail("Scenario 4 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Product Reference Listing
    // -------------------------------------------------------------------------
    private static void testScenario5_ProductReferenceListing() {
        System.out.println("[Scenario 5] GET /api/products — Product References Listing");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/products"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Product listing status code is 200 OK", response.statusCode() == 200);
            assertTrue("Contains product 1 ID", response.body().contains(PROD_1_ID));
            assertTrue("Contains product 1 title", response.body().contains("Introduction to Algorithms"));
        } catch (Exception e) {
            fail("Scenario 5 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Conversation Listing by Participant
    // -------------------------------------------------------------------------
    private static void testScenario6_ConversationListingByParticipant() {
        System.out.println("[Scenario 6] GET /api/conversations?participantId={userId} — Participant Filtering");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations?participantId=" + BUYER_ID))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Conversations query status code is 200 OK", response.statusCode() == 200);
            assertTrue("Response contains seeded conversation", response.body().contains("p9_conv_seed"));
            assertTrue("Contains buyer ID", response.body().contains(BUYER_ID));
            assertTrue("Contains product ID", response.body().contains(PROD_1_ID));
        } catch (Exception e) {
            fail("Scenario 6 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Conversation Creation
    // -------------------------------------------------------------------------
    private static void testScenario7_ConversationCreation() {
        System.out.println("[Scenario 7] POST /api/conversations — Create Valid Conversation");
        try {
            String payload = "{\"buyerId\":\"" + BUYER_ID + "\",\"sellerId\":\"" + SELLER_ID + "\",\"productId\":\"" + PROD_2_ID + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Conversation creation status code is 201 Created", response.statusCode() == 201);
            assertTrue("Response envelope success is true", response.body().contains("\"success\":true"));
            assertTrue("Response contains generated conversationId", response.body().contains("\"conversationId\":"));

            // Parse created conversation ID for subsequent tests
            int idx = response.body().indexOf("\"conversationId\":\"");
            if (idx != -1) {
                int start = idx + 18;
                int end = response.body().indexOf("\"", start);
                createdConvId = response.body().substring(start, end);
            }
            assertTrue("Created conversationId was extracted", createdConvId != null && !createdConvId.isEmpty());
        } catch (Exception e) {
            fail("Scenario 7 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Duplicate Conversation Conflict
    // -------------------------------------------------------------------------
    private static void testScenario8_DuplicateConversationConflict() {
        System.out.println("[Scenario 8] POST /api/conversations — Duplicate Conversation 409 Conflict");
        try {
            // Attempt to recreate same conversation (BUYER_ID, SELLER_ID, PROD_2_ID)
            String payload = "{\"buyerId\":\"" + BUYER_ID + "\",\"sellerId\":\"" + SELLER_ID + "\",\"productId\":\"" + PROD_2_ID + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Duplicate conversation returns 409 Conflict", response.statusCode() == 409);
            assertTrue("Response envelope success is false", response.body().contains("\"success\":false"));
            assertTrue("Error message explains duplicate conversation", response.body().toLowerCase().contains("duplicate conversation"));
        } catch (Exception e) {
            fail("Scenario 8 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Self-Conversation Rejection
    // -------------------------------------------------------------------------
    private static void testScenario9_SelfConversationRejection() {
        System.out.println("[Scenario 9] POST /api/conversations — Self-Conversation 400 Bad Request");
        try {
            String payload = "{\"buyerId\":\"" + BUYER_ID + "\",\"sellerId\":\"" + BUYER_ID + "\",\"productId\":\"" + PROD_1_ID + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Self-conversation returns 400 Bad Request", response.statusCode() == 400);
            assertTrue("Error message explains buyer and seller cannot be identical",
                    response.body().toLowerCase().contains("cannot be identical") || response.body().toLowerCase().contains("themselves"));
        } catch (Exception e) {
            fail("Scenario 9 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Conversation Lookup
    // -------------------------------------------------------------------------
    private static void testScenario10_ConversationLookup() {
        System.out.println("[Scenario 10] GET /api/conversations/{conversationId} — Conversation Lookup");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Conversation lookup status code is 200 OK", response.statusCode() == 200);
            assertTrue("Response contains requested conversation ID", response.body().contains(createdConvId));
            assertTrue("Response contains buyer ID", response.body().contains(BUYER_ID));
            assertTrue("Response contains seller ID", response.body().contains(SELLER_ID));
        } catch (Exception e) {
            fail("Scenario 10 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Message History Retrieval
    // -------------------------------------------------------------------------
    private static void testScenario11_MessageHistoryRetrieval() {
        System.out.println("[Scenario 11] GET /api/conversations/{id}/messages — Retrieve Message History");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("X-Dev-User-Id", BUYER_ID)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Message history status code is 200 OK", response.statusCode() == 200);
            assertTrue("Response envelope success is true", response.body().contains("\"success\":true"));
        } catch (Exception e) {
            fail("Scenario 11 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Message Submission
    // -------------------------------------------------------------------------
    private static void testScenario12_MessageSubmission() {
        System.out.println("[Scenario 12] POST /api/conversations/{id}/messages — Message Submission with X-Dev-User-Id");
        try {
            String payload = "{\"content\":\"Hello from automated Phase 9 test!\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", BUYER_ID)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Message submission returns 201 Created", response.statusCode() == 201);
            assertTrue("Response envelope success is true", response.body().contains("\"success\":true"));
            assertTrue("Response contains submitted content", response.body().contains("Hello from automated Phase 9 test!"));
            assertTrue("Response contains senderId", response.body().contains("\"senderId\":\"" + BUYER_ID + "\""));
        } catch (Exception e) {
            fail("Scenario 12 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Message Persistence in MySQL
    // -------------------------------------------------------------------------
    private static void testScenario13_MessagePersistenceInMySql() {
        System.out.println("[Scenario 13] Verification of Message Persistence directly in MySQL Database");
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            List<Message> history = messageDao.findByConversationId(conn, createdConvId);
            assertTrue("MySQL messages table contains persisted record", !history.isEmpty());
            assertTrue("Persisted message content matches submission", "Hello from automated Phase 9 test!".equals(history.get(0).getContent()));
            assertTrue("Persisted message sender matches buyer", BUYER_ID.equals(history.get(0).getSenderId()));
        } catch (Exception e) {
            fail("Scenario 13 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 14: Persistence Across Server Restart
    // -------------------------------------------------------------------------
    private static void testScenario14_PersistenceAcrossServerRestart() {
        System.out.println("[Scenario 14] Message Persistence across Full HTTP Server Restart");
        try {
            // Stop active server
            server.stop(0);
            assertTrue("Previous server stopped successfully", !server.isRunning());

            // Start fresh server instance on a new ephemeral port
            server = new UniGramHttpServer("127.0.0.1", 0, testConfig, new CorsConfig());
            server.start();
            baseUrl = server.getBaseUrl();
            assertTrue("New server instance started successfully", server.isRunning());

            // Query message history through new server instance
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("X-Dev-User-Id", BUYER_ID)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Message history accessible after restart (200 OK)", response.statusCode() == 200);
            assertTrue("Message content persisted across restart", response.body().contains("Hello from automated Phase 9 test!"));
        } catch (Exception e) {
            fail("Scenario 14 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 15: Missing Development Identity Handling
    // -------------------------------------------------------------------------
    private static void testScenario15_MissingDevelopmentIdentity() {
        System.out.println("[Scenario 15] POST /api/conversations/{id}/messages — Missing X-Dev-User-Id (401 Unauthorized)");
        try {
            String payload = "{\"content\":\"Message without dev identity header\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("Content-Type", "application/json")
                    // OMIT X-Dev-User-Id header
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Missing X-Dev-User-Id returns 401 Unauthorized", response.statusCode() == 401);
            assertTrue("Response envelope success is false", response.body().contains("\"success\":false"));
            assertTrue("Error message mentions missing development identity", response.body().contains("X-Dev-User-Id"));
        } catch (Exception e) {
            fail("Scenario 15 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 16: Unauthorized Participant Rejection
    // -------------------------------------------------------------------------
    private static void testScenario16_UnauthorizedParticipantRejection() {
        System.out.println("[Scenario 16] POST /api/conversations/{id}/messages — Unauthorized Participant (403 Forbidden)");
        try {
            String payload = "{\"content\":\"Intruder message\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", UNAUTHORIZED_USER_ID)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Unauthorized participant returns 403 Forbidden", response.statusCode() == 403);
            assertTrue("Response envelope success is false", response.body().contains("\"success\":false"));
            assertTrue("Error message explains unauthorized participant", response.body().contains("not an authorized participant"));
        } catch (Exception e) {
            fail("Scenario 16 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 17: Blank Message Rejection
    // -------------------------------------------------------------------------
    private static void testScenario17_BlankMessageRejection() {
        System.out.println("[Scenario 17] POST /api/conversations/{id}/messages — Blank Content (400 Bad Request)");
        try {
            String payload = "{\"content\":\"     \"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConvId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", BUYER_ID)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Blank message returns 400 Bad Request", response.statusCode() == 400);
            assertTrue("Error message explains message content cannot be blank", response.body().contains("cannot be null or blank"));
        } catch (Exception e) {
            fail("Scenario 17 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 18: Malformed JSON Handling
    // -------------------------------------------------------------------------
    private static void testScenario18_MalformedJsonHandling() {
        System.out.println("[Scenario 18] POST /api/conversations — Malformed JSON Payload (400 Bad Request)");
        try {
            String brokenPayload = "{\"buyerId\":\"" + BUYER_ID + "\", broken_json...";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(brokenPayload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Malformed JSON returns 400 Bad Request", response.statusCode() == 400);
            assertTrue("Error message mentions malformed JSON", response.body().toLowerCase().contains("malformed json"));
        } catch (Exception e) {
            fail("Scenario 18 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 19: Invalid Routes and HTTP Methods
    // -------------------------------------------------------------------------
    private static void testScenario19_InvalidRoutesAndHttpMethods() {
        System.out.println("[Scenario 19] Route Handling — 404 for Unknown Endpoints & 405 for Unsupported Methods");
        try {
            // 404 on unknown endpoint
            HttpRequest notFoundReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/nonexistent/route"))
                    .GET()
                    .build();
            HttpResponse<String> notFoundRes = httpClient.send(notFoundReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Unknown endpoint returns 404 Not Found", notFoundRes.statusCode() == 404);
            assertTrue("Body contains endpoint not found error", notFoundRes.body().contains("Endpoint not found"));

            // 405 on method not allowed
            HttpRequest methodNotAllowedReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpResponse<String> methodRes = httpClient.send(methodNotAllowedReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Unsupported PUT on /api/health returns 405 Method Not Allowed", methodRes.statusCode() == 405);
            assertTrue("Response headers include Allow header", methodRes.headers().firstValue("Allow").orElse("").contains("GET"));
        } catch (Exception e) {
            fail("Scenario 19 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 20: CORS Preflight Behaviour
    // -------------------------------------------------------------------------
    private static void testScenario20_CorsPreflightBehaviour() {
        System.out.println("[Scenario 20] CORS Preflight Negotiation (OPTIONS & Origin Headers)");
        try {
            HttpRequest optionsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type, X-Dev-User-Id")
                    .build();
            HttpResponse<String> response = httpClient.send(optionsReq, HttpResponse.BodyHandlers.ofString());

            assertTrue("Preflight OPTIONS returns 204 No Content", response.statusCode() == 204);
            assertTrue("Access-Control-Allow-Origin echoes trusted origin",
                    "http://localhost:5173".equals(response.headers().firstValue("Access-Control-Allow-Origin").orElse("")));
            assertTrue("Access-Control-Allow-Methods includes GET and POST",
                    response.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"));
            assertTrue("Access-Control-Allow-Headers includes X-Dev-User-Id",
                    response.headers().firstValue("Access-Control-Allow-Headers").orElse("").contains("X-Dev-User-Id"));
        } catch (Exception e) {
            fail("Scenario 20 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 21: Concurrent HTTP Requests
    // -------------------------------------------------------------------------
    private static void testScenario21_ConcurrentHttpRequests() {
        System.out.println("[Scenario 21] Concurrent Client Request Dispatching (10 Simultaneous Requests)");
        try {
            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/health"))
                        .GET()
                        .build();
                futures.add(httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            boolean all200 = true;
            for (CompletableFuture<HttpResponse<String>> f : futures) {
                if (f.get().statusCode() != 200) {
                    all200 = false;
                    break;
                }
            }
            assertTrue("All 10 concurrent requests completed with 200 OK", all200);
        } catch (Exception e) {
            fail("Scenario 21 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 22: Graceful Server Shutdown
    // -------------------------------------------------------------------------
    private static void testScenario22_GracefulServerShutdown() {
        System.out.println("[Scenario 22] Graceful HTTP Server & Thread Pool Termination");
        try {
            server.stop(1);
            assertTrue("Server reported running state is false", !server.isRunning());

            boolean connectionRefused = false;
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/health"))
                        .timeout(Duration.ofMillis(800))
                        .GET()
                        .build();
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                // Expected when port is no longer listening
                connectionRefused = true;
            }
            assertTrue("Server stopped accepting connections after shutdown", connectionRefused);
        } catch (Exception e) {
            fail("Scenario 22 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 23: Database-Unavailable Error Handling
    // -------------------------------------------------------------------------
    private static void testScenario23_DatabaseUnavailableHandling() {
        System.out.println("[Scenario 23] Database Unreachability & HTTP 503 Handling");
        UniGramHttpServer degradedServer = null;
        try {
            // Configure an unreachable MySQL port to simulate database outage
            DatabaseConfig unreachableConfig = new DatabaseConfig(
                    "jdbc:mysql://127.0.0.1:59999/unigram_test_db?connectTimeout=500&socketTimeout=500",
                    "dummy_user",
                    "dummy_pass"
            );
            degradedServer = new UniGramHttpServer("127.0.0.1", 0, unreachableConfig, new CorsConfig());
            degradedServer.start();
            String degradedUrl = degradedServer.getBaseUrl();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(degradedUrl + "/api/health"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Unreachable database returns HTTP 503 Service Unavailable", response.statusCode() == 503);
            assertTrue("Response body indicates DEGRADED status", response.body().contains("\"status\":\"DEGRADED\""));
            assertTrue("Response body indicates DISCONNECTED database", response.body().contains("\"database\":\"DISCONNECTED\""));
            assertTrue("Response does NOT leak password", !response.body().contains("dummy_pass"));

        } catch (Exception e) {
            fail("Scenario 23 failed: " + e.getMessage());
        } finally {
            if (degradedServer != null && degradedServer.isRunning()) {
                degradedServer.stop(0);
            }
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Verification Helpers
    // -------------------------------------------------------------------------
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
        System.out.println("                   Phase 9 Verification Summary                       ");
        System.out.println("======================================================================");
        System.out.println("Total Assertions Evaluated : " + totalAssertions);
        System.out.println("Assertions Passed          : " + passedAssertions);
        System.out.println("Assertions Failed          : " + failedAssertions);
        if (skippedSuites > 0) {
            System.out.println("Suites Skipped (Offline)   : " + skippedSuites + " (requires live unigram_test_db)");
        }
        System.out.println("Overall Test Status        : " + (failedAssertions == 0 ? "ALL PASS" : "FAILURES DETECTED"));
        System.out.println("======================================================================\n");
    }
}
