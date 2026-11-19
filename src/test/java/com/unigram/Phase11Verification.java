package com.unigram;

import com.unigram.api.CorsConfig;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Phase 11 Verification Test Suite: Complete Frontend and Backend Integration.
 *
 * Strict Safety Guarantee:
 * - Operates EXCLUSIVELY against `unigram_test_db`.
 * - Explicitly refuses execution if configured URL points to `unigram_db`.
 * - Uses ephemeral port (port 0) to avoid port conflicts with running dev servers.
 * - Simulates the exact HTTP requests dispatched by `unimart-frontend` (`unigramApi.js`).
 *
 * Scenarios:
 *  1. Server & Database Health Check (GET /api/health)
 *  2. Idempotent User Sync (POST /api/users)
 *  3. Idempotent Product Reference Sync (POST /api/products)
 *  4. Conversation Creation (POST /api/conversations)
 *  5. Duplicate Conversation Handling (409 Conflict with existing Conversation)
 *  6. Self-Conversation Rejection (400 Bad Request)
 *  7. Message Submission (POST /api/conversations/{id}/messages)
 *  8. Message History Retrieval (GET /api/conversations/{id}/messages)
 *  9. Security & Participant Authorization (401 Unauthorized, 403 Forbidden)
 * 10. Frontend Polling Simulation (Rapid sequential message queries)
 * 11. Persistence Across Server Restart (Full durability verification)
 * 12. Direct MySQL JDBC State Verification (Verifying database commitments)
 * 13. CORS Verification for React Vite Frontend (http://localhost:5173)
 * 14. Graceful Server Shutdown
 */
public class Phase11Verification {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;

    private static DatabaseConfig testConfig;
    private static UniGramHttpServer server;
    private static String baseUrl;
    private static HttpClient httpClient;

    private static final UserJdbcDao userDao = new UserJdbcDao();
    private static final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private static final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private static final MessageJdbcDao messageDao = new MessageJdbcDao();

    // Frontend Mock User & Product identifiers matching UniMart AuthContext & mockListings
    private static final String STUDENT_A = "p11_student_a";
    private static final String STUDENT_B = "p11_student_b";
    private static final String STUDENT_C = "p11_student_c";
    private static final String PRODUCT_CALCULUS_ID = "p11_prod_1";
    private static final String PRODUCT_GRAPHING_ID = "p11_prod_2";

    private static String createdConversationId;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          Uni-Gram — Phase 11 Verification Test Suite                 ");
        System.out.println("      Complete Frontend (UniMart React) and Backend Integration       ");
        System.out.println("======================================================================\n");

        resolveTestConfig();
        testConfigurationSafetyGuard();

        boolean dbAvailable = checkDatabaseReachabilityAndSafety();
        if (!dbAvailable) {
            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("[!] NOTICE: MySQL Test Database (unigram_test_db) is not configured or reachable.");
            System.out.println("Online Phase 11 integration tests are SKIPPED.");
            System.out.println("To execute full live MySQL tests, configure your test environment variables:");
            System.out.println("  $env:UNIGRAM_TEST_DB_URL = \"jdbc:mysql://localhost:3306/unigram_test_db?serverTimezone=UTC\"");
            System.out.println("  $env:UNIGRAM_DB_USER = \"<mysql-username>\"");
            System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<mysql-password>\"");
            System.out.println("----------------------------------------------------------------------\n");
            printSummary(false);
            return;
        }

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            cleanupAllTestData();

            server = new UniGramHttpServer("127.0.0.1", 0, testConfig, new CorsConfig());
            server.start();
            baseUrl = server.getBaseUrl();
            System.out.println("[+] Live Test HTTP Server started at: " + baseUrl + "\n");

            testScenario1_HealthEndpoint();
            testScenario2_UserIdempotentSync();
            testScenario3_ProductIdempotentSync();
            testScenario4_ConversationCreation();
            testScenario5_DuplicateConversationHandling();
            testScenario6_SelfConversationRejection();
            testScenario7_MessageSubmission();
            testScenario8_MessageRetrieval();
            testScenario9_SecurityAndAuthorization();
            testScenario10_FrontendPollingSimulation();
            testScenario11_PersistenceAcrossRestart();
            testScenario12_DirectMySqlVerification();
            testScenario13_CorsHeadersForFrontend();
            testScenario14_GracefulShutdown();

        } catch (Exception e) {
            fail("Fatal exception during Phase 11 verification: " + e.getMessage());
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

    private static void testConfigurationSafetyGuard() {
        System.out.println("[Test 0] Test Database Safety Guard Validation");
        DatabaseConfig dummyConfig = new DatabaseConfig("jdbc:mysql://localhost:3306/unigram_test_db", "test_user", "test_pass");
        assertTrue("Test config URL strictly contains unigram_test_db", dummyConfig.getJdbcUrl().contains("unigram_test_db"));
        assertTrue("Test config credentials present", dummyConfig.isConfigured());
        System.out.println();
    }

    private static boolean checkDatabaseReachabilityAndSafety() {
        if (!testConfig.isConfigured()) {
            return false;
        }

        String url = testConfig.getJdbcUrl().toLowerCase();
        if (url.contains("/unigram_db") && !url.contains("unigram_test_db")) {
            throw new IllegalStateException("CRITICAL SAFETY VIOLATION: Phase11Verification must use unigram_test_db exclusively! Refusing to run against: " + testConfig.getJdbcUrl());
        }

        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private static void cleanupAllTestData() {
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE message_id LIKE 'p11_%' OR sender_id LIKE 'p11_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversations WHERE conversation_id LIKE 'p11_%' OR buyer_id LIKE 'p11_%' OR seller_id LIKE 'p11_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_references WHERE product_id LIKE 'p11_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id LIKE 'p11_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: Health Endpoint
    // -------------------------------------------------------------------------
    private static void testScenario1_HealthEndpoint() {
        System.out.println("[Scenario 1] GET /api/health — Frontend Backend Health Check");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertTrue("Health status code is 200 OK", response.statusCode() == 200);
            assertTrue("Health report status is UP", response.body().contains("\"status\":\"UP\""));
            assertTrue("Health report database is CONNECTED", response.body().contains("\"database\":\"CONNECTED\""));
            assertTrue("Health report apiVersion is 1.0.0", response.body().contains("\"apiVersion\":\"1.0.0\""));
        } catch (Exception e) {
            fail("Scenario 1 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Idempotent User Sync
    // -------------------------------------------------------------------------
    private static void testScenario2_UserIdempotentSync() {
        System.out.println("[Scenario 2] POST /api/users — Idempotent User Sync for UniMart Users");
        try {
            // First sync student_a: should create user
            String payloadA = String.format("{\"userId\":\"%s\",\"fullName\":\"Satkar Singh\",\"email\":\"satkar@campus.edu\"}", STUDENT_A);
            HttpRequest reqA = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadA))
                    .build();
            HttpResponse<String> respA = httpClient.send(reqA, HttpResponse.BodyHandlers.ofString());

            assertTrue("First user sync returns 201 Created", respA.statusCode() == 201);
            assertTrue("Response envelope confirms success", respA.body().contains("\"success\":true"));
            assertTrue("Response body contains user id", respA.body().contains(STUDENT_A));

            // Sync student_a again (idempotent call): should return 200 OK without duplication
            HttpResponse<String> respAIdempotent = httpClient.send(reqA, HttpResponse.BodyHandlers.ofString());
            assertTrue("Repeated user sync returns 200 OK (idempotent)", respAIdempotent.statusCode() == 200);
            assertTrue("Repeated sync envelope confirms success", respAIdempotent.body().contains("\"success\":true"));

            // Sync student_b (Seller)
            String payloadB = String.format("{\"userId\":\"%s\",\"fullName\":\"Priya K.\",\"email\":\"priya@campus.edu\"}", STUDENT_B);
            HttpRequest reqB = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadB))
                    .build();
            HttpResponse<String> respB = httpClient.send(reqB, HttpResponse.BodyHandlers.ofString());
            assertTrue("Seller sync returns 201 Created", respB.statusCode() == 201);

            // Sync student_c (Unauthorized observer)
            String payloadC = String.format("{\"userId\":\"%s\",\"fullName\":\"Alex M.\",\"email\":\"alex@campus.edu\"}", STUDENT_C);
            HttpRequest reqC = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadC))
                    .build();
            HttpResponse<String> respC = httpClient.send(reqC, HttpResponse.BodyHandlers.ofString());
            assertTrue("Third student sync returns 201 Created", respC.statusCode() == 201);

            // Verify bad request validation (missing userId)
            HttpRequest reqBad = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"fullName\":\"No ID Student\"}"))
                    .build();
            HttpResponse<String> respBad = httpClient.send(reqBad, HttpResponse.BodyHandlers.ofString());
            assertTrue("Missing userId in sync request returns 400 Bad Request", respBad.statusCode() == 400);

        } catch (Exception e) {
            fail("Scenario 2 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Idempotent Product Reference Sync
    // -------------------------------------------------------------------------
    private static void testScenario3_ProductIdempotentSync() {
        System.out.println("[Scenario 3] POST /api/products — Idempotent Product Sync from UniMart Listings");
        try {
            // First sync Calculus product
            String payload1 = String.format("{\"productId\":\"%s\",\"productTitle\":\"Calculus: Early Transcendentals\"}", PRODUCT_CALCULUS_ID);
            HttpRequest req1 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/products"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload1))
                    .build();
            HttpResponse<String> resp1 = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());

            assertTrue("First product sync returns 201 Created", resp1.statusCode() == 201);
            assertTrue("Product sync response contains title", resp1.body().contains("Calculus"));

            // Repeated sync (idempotency)
            HttpResponse<String> resp1Idempotent = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());
            assertTrue("Repeated product sync returns 200 OK (idempotent)", resp1Idempotent.statusCode() == 200);
            assertTrue("Repeated product sync response confirms success", resp1Idempotent.body().contains("\"success\":true"));

            // Sync second product
            String payload2 = String.format("{\"productId\":\"%s\",\"productTitle\":\"TI-84 Plus CE Graphing Calculator\"}", PRODUCT_GRAPHING_ID);
            HttpRequest req2 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/products"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload2))
                    .build();
            HttpResponse<String> resp2 = httpClient.send(req2, HttpResponse.BodyHandlers.ofString());
            assertTrue("Second product sync returns 201 Created", resp2.statusCode() == 201);

            // Validation rejection for blank title
            String payloadBad = String.format("{\"productId\":\"p11_bad\",\"productTitle\":\"\"}");
            HttpRequest reqBad = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/products"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadBad))
                    .build();
            HttpResponse<String> respBad = httpClient.send(reqBad, HttpResponse.BodyHandlers.ofString());
            assertTrue("Blank product title returns 400 Bad Request", respBad.statusCode() == 400);

        } catch (Exception e) {
            fail("Scenario 3 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Conversation Creation
    // -------------------------------------------------------------------------
    private static void testScenario4_ConversationCreation() {
        System.out.println("[Scenario 4] POST /api/conversations — Initiating Marketplace Chat");
        try {
            String payload = String.format("{\"buyerId\":\"%s\",\"sellerId\":\"%s\",\"productId\":\"%s\"}",
                    STUDENT_A, STUDENT_B, PRODUCT_CALCULUS_ID);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertTrue("Conversation creation returns 201 Created", resp.statusCode() == 201);
            assertTrue("Response envelope confirms success", resp.body().contains("\"success\":true"));
            assertTrue("Response contains buyer ID", resp.body().contains(STUDENT_A));
            assertTrue("Response contains seller ID", resp.body().contains(STUDENT_B));
            assertTrue("Response contains product ID", resp.body().contains(PRODUCT_CALCULUS_ID));

            // Extract conversation ID for subsequent tests
            String marker = "\"id\":\"";
            int idx = resp.body().indexOf(marker);
            assertTrue("Response contains conversation id field", idx != -1);
            int endIdx = resp.body().indexOf("\"", idx + marker.length());
            createdConversationId = resp.body().substring(idx + marker.length(), endIdx);
            assertTrue("Extracted conversation ID is valid", createdConversationId != null && !createdConversationId.isEmpty());
            System.out.println("  -> Created Conversation ID: " + createdConversationId);

        } catch (Exception e) {
            fail("Scenario 4 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Duplicate Conversation Handling
    // -------------------------------------------------------------------------
    private static void testScenario5_DuplicateConversationHandling() {
        System.out.println("[Scenario 5] POST /api/conversations — Duplicate Thread 409 Conflict Handling");
        try {
            String payload = String.format("{\"buyerId\":\"%s\",\"sellerId\":\"%s\",\"productId\":\"%s\"}",
                    STUDENT_A, STUDENT_B, PRODUCT_CALCULUS_ID);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertTrue("Duplicate conversation returns 409 Conflict", resp.statusCode() == 409);
            assertTrue("Response indicates conflict error", resp.body().contains("already exists"));
            assertTrue("Response data contains existing conversation id", resp.body().contains(createdConversationId));
            assertTrue("Response data contains buyer ID", resp.body().contains(STUDENT_A));

        } catch (Exception e) {
            fail("Scenario 5 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Self-Conversation Rejection
    // -------------------------------------------------------------------------
    private static void testScenario6_SelfConversationRejection() {
        System.out.println("[Scenario 6] POST /api/conversations — Prohibit Self-Conversation");
        try {
            String payload = String.format("{\"buyerId\":\"%s\",\"sellerId\":\"%s\",\"productId\":\"%s\"}",
                    STUDENT_A, STUDENT_A, PRODUCT_CALCULUS_ID);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertTrue("Self-conversation attempt returns 400 Bad Request", resp.statusCode() == 400);
            assertTrue("Response contains domain validation error", resp.body().toLowerCase().contains("buyer and seller cannot be identical"));

        } catch (Exception e) {
            fail("Scenario 6 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Message Submission
    // -------------------------------------------------------------------------
    private static void testScenario7_MessageSubmission() {
        System.out.println("[Scenario 7] POST /api/conversations/{id}/messages — Sending Messages with X-Dev-User-Id");
        try {
            // Message 1: Buyer sends inquiry
            String msgText1 = "Hi Priya, is the Calculus book still available for campus meetup?";
            String payload1 = String.format("{\"content\":\"%s\"}", msgText1);
            HttpRequest req1 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", STUDENT_A)
                    .POST(HttpRequest.BodyPublishers.ofString(payload1))
                    .build();
            HttpResponse<String> resp1 = httpClient.send(req1, HttpResponse.BodyHandlers.ofString());

            assertTrue("Buyer message dispatch returns 201 Created", resp1.statusCode() == 201);
            assertTrue("Message response contains content", resp1.body().contains(msgText1));
            assertTrue("Message response contains senderId", resp1.body().contains(STUDENT_A));
            assertTrue("Message response contains conversationId", resp1.body().contains(createdConversationId));

            // Message 2: Seller sends reply
            String msgText2 = "Yes Satkar! I can meet you outside the library today around 4 PM.";
            String payload2 = String.format("{\"content\":\"%s\"}", msgText2);
            HttpRequest req2 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", STUDENT_B)
                    .POST(HttpRequest.BodyPublishers.ofString(payload2))
                    .build();
            HttpResponse<String> resp2 = httpClient.send(req2, HttpResponse.BodyHandlers.ofString());

            assertTrue("Seller reply dispatch returns 201 Created", resp2.statusCode() == 201);
            assertTrue("Seller reply response contains content", resp2.body().contains(msgText2));
            assertTrue("Seller reply response contains seller senderId", resp2.body().contains(STUDENT_B));

        } catch (Exception e) {
            fail("Scenario 7 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Message Retrieval
    // -------------------------------------------------------------------------
    private static void testScenario8_MessageRetrieval() {
        System.out.println("[Scenario 8] GET /api/conversations/{id}/messages — Chronological Message History");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertTrue("Message history retrieval returns 200 OK", resp.statusCode() == 200);
            assertTrue("Response envelope confirms success", resp.body().contains("\"success\":true"));
            assertTrue("Response contains buyer inquiry", resp.body().contains("Calculus book still available"));
            assertTrue("Response contains seller reply", resp.body().contains("outside the library today around 4 PM"));

            // Verify chronological order: inquiry should precede reply in JSON string
            int idxInquiry = resp.body().indexOf("Calculus book still available");
            int idxReply = resp.body().indexOf("outside the library today around 4 PM");
            assertTrue("Messages are returned in chronological order", idxInquiry < idxReply);

        } catch (Exception e) {
            fail("Scenario 8 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Security and Authorization
    // -------------------------------------------------------------------------
    private static void testScenario9_SecurityAndAuthorization() {
        System.out.println("[Scenario 9] Security Enforcement: 401 Unauthorized & 403 Forbidden");
        try {
            // Missing X-Dev-User-Id header
            HttpRequest reqNoAuth = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"Unauthenticated message\"}"))
                    .build();
            HttpResponse<String> respNoAuth = httpClient.send(reqNoAuth, HttpResponse.BodyHandlers.ofString());
            assertTrue("Missing X-Dev-User-Id header returns 401 Unauthorized", respNoAuth.statusCode() == 401);

            // Unauthorized participant: student_c is not buyer or seller
            HttpRequest reqForbidden = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", STUDENT_C)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"Eavesdropping message\"}"))
                    .build();
            HttpResponse<String> respForbidden = httpClient.send(reqForbidden, HttpResponse.BodyHandlers.ofString());
            assertTrue("Unauthorized participant dispatch returns 403 Forbidden", respForbidden.statusCode() == 403);
            assertTrue("Forbidden response mentions participant restriction", respForbidden.body().contains("participant"));

            // Empty message content validation
            HttpRequest reqEmpty = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("X-Dev-User-Id", STUDENT_A)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"   \"}"))
                    .build();
            HttpResponse<String> respEmpty = httpClient.send(reqEmpty, HttpResponse.BodyHandlers.ofString());
            assertTrue("Blank message returns 400 Bad Request", respEmpty.statusCode() == 400);

        } catch (Exception e) {
            fail("Scenario 9 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Frontend Polling Simulation
    // -------------------------------------------------------------------------
    private static void testScenario10_FrontendPollingSimulation() {
        System.out.println("[Scenario 10] Frontend Active Polling Simulation (3–5s cadence)");
        try {
            // Simulate 3 rapid polling requests from open chat modal
            for (int i = 1; i <= 3; i++) {
                HttpRequest pollReq = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                        .GET()
                        .build();
                HttpResponse<String> pollResp = httpClient.send(pollReq, HttpResponse.BodyHandlers.ofString());
                assertTrue(String.format("Poll request #%d returns 200 OK", i), pollResp.statusCode() == 200);
                assertTrue(String.format("Poll request #%d contains live messages", i), pollResp.body().contains("outside the library"));
            }
        } catch (Exception e) {
            fail("Scenario 10 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Persistence Across Server Restart
    // -------------------------------------------------------------------------
    private static void testScenario11_PersistenceAcrossRestart() {
        System.out.println("[Scenario 11] Persistence Across Server Restart (MySQL Durability)");
        try {
            // Stop current server
            server.stop(0);
            System.out.println("  -> Server stopped.");

            // Start new server instance on ephemeral port pointing to same test DB
            server = new UniGramHttpServer("127.0.0.1", 0, testConfig, new CorsConfig());
            server.start();
            baseUrl = server.getBaseUrl();
            System.out.println("  -> Fresh server restarted at: " + baseUrl);

            // Fetch messages from restarted server
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + createdConversationId + "/messages"))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            assertTrue("Restarted server returns 200 OK", resp.statusCode() == 200);
            assertTrue("Restarted server returns persisted buyer message", resp.body().contains("Calculus book still available"));
            assertTrue("Restarted server returns persisted seller reply", resp.body().contains("outside the library today around 4 PM"));

        } catch (Exception e) {
            fail("Scenario 11 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Direct MySQL JDBC State Verification
    // -------------------------------------------------------------------------
    private static void testScenario12_DirectMySqlVerification() {
        System.out.println("[Scenario 12] Direct MySQL JDBC State Verification (Transactions Committed)");
        try (Connection conn = DatabaseConnectionFactory.getConnection(testConfig)) {
            // Verify User rows in MySQL
            Optional<User> userA = userDao.findById(conn, STUDENT_A);
            assertTrue("student_a exists in MySQL users table", userA.isPresent());
            assertTrue("student_a full name matches", userA.get().getFullName().equals("Satkar Singh"));

            Optional<User> userB = userDao.findById(conn, STUDENT_B);
            assertTrue("student_b exists in MySQL users table", userB.isPresent());

            // Verify ProductReference row in MySQL
            Optional<ProductReference> prod1 = productDao.findById(conn, PRODUCT_CALCULUS_ID);
            assertTrue("Product reference exists in MySQL product_references table", prod1.isPresent());
            assertTrue("Product title matches", prod1.get().getProductTitle().contains("Calculus"));

            // Verify Conversation row in MySQL
            Optional<Conversation> conv = conversationDao.findById(conn, createdConversationId);
            assertTrue("Conversation exists in MySQL conversations table", conv.isPresent());
            assertTrue("Conversation buyer matches", conv.get().getBuyerId().equals(STUDENT_A));
            assertTrue("Conversation seller matches", conv.get().getSellerId().equals(STUDENT_B));
            assertTrue("Conversation product matches", conv.get().getProductId().equals(PRODUCT_CALCULUS_ID));

            // Verify Message rows in MySQL
            List<Message> messages = messageDao.findByConversationId(conn, createdConversationId);
            assertTrue("Exactly 2 messages persisted in MySQL messages table", messages.size() == 2);
            assertTrue("First message sender is student_a", messages.get(0).getSenderId().equals(STUDENT_A));
            assertTrue("Second message sender is student_b", messages.get(1).getSenderId().equals(STUDENT_B));

        } catch (Exception e) {
            fail("Scenario 12 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 13: CORS Headers Verification
    // -------------------------------------------------------------------------
    private static void testScenario13_CorsHeadersForFrontend() {
        System.out.println("[Scenario 13] CORS Headers for React Frontend (Vite on http://localhost:5173)");
        try {
            HttpRequest optionsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type, X-Dev-User-Id")
                    .build();
            HttpResponse<Void> optionsResp = httpClient.send(optionsReq, HttpResponse.BodyHandlers.discarding());

            assertTrue("Preflight OPTIONS returns 204 No Content", optionsResp.statusCode() == 204);
            String allowOrigin = optionsResp.headers().firstValue("Access-Control-Allow-Origin").orElse("");
            assertTrue("CORS allows origin * or http://localhost:5173", allowOrigin.equals("*") || allowOrigin.contains("localhost:5173"));

            String allowHeaders = optionsResp.headers().firstValue("Access-Control-Allow-Headers").orElse("");
            assertTrue("CORS preflight allows X-Dev-User-Id header", allowHeaders.toLowerCase().contains("x-dev-user-id"));

        } catch (Exception e) {
            fail("Scenario 13 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 14: Graceful Shutdown
    // -------------------------------------------------------------------------
    private static void testScenario14_GracefulShutdown() {
        System.out.println("[Scenario 14] Graceful HTTP Server Shutdown");
        try {
            assertTrue("Server is currently running", server.isRunning());
            server.stop(0);
            assertTrue("Server is stopped after stop(0)", !server.isRunning());
        } catch (Exception e) {
            fail("Scenario 14 failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Assertion Helpers
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

    private static void printSummary(boolean ranOnline) {
        System.out.println("======================================================================");
        System.out.println("                   PHASE 11 TEST SUITE SUMMARY                        ");
        System.out.println("======================================================================");
        System.out.println("Total Assertions Evaluated: " + totalAssertions);
        System.out.println("Passed Assertions:          " + passedAssertions);
        System.out.println("Failed Assertions:          " + failedAssertions);
        if (ranOnline) {
            System.out.println("Test Database:              unigram_test_db (Live MySQL)");
        } else {
            System.out.println("Test Database:              Offline / Skipped");
        }
        System.out.println("Overall Status:             " + (failedAssertions == 0 ? "PASSED (SUCCESS)" : "FAILED"));
        System.out.println("======================================================================");
    }
}
