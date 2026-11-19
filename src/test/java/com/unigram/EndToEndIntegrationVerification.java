package com.unigram;

import com.unigram.api.UniGramHttpServer;
import com.unigram.database.DatabaseConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * End-to-End Cross-Stack Integration and Multi-User Journey Verification.
 * Tests:
 * 1. Dual student registration & authentication.
 * 2. Listing creation with moderation.
 * 3. Buyer-seller thread initiation and message exchange.
 * 4. Wishlist addition & account isolation.
 * 5. Admin moderation approval.
 * 6. Server restart state verification.
 * 7. Outage simulation & durable write rejection.
 */
public class EndToEndIntegrationVerification {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    private static void check(String description, boolean condition, String details) {
        if (condition) {
            totalPassed++;
            System.out.println("  [PASS] " + description);
        } else {
            totalFailed++;
            System.err.println("  [FAIL] " + description + " -> " + details);
        }
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("       UniMart + Uni-Gram End-to-End Journey & Persistence Test       ");
        System.out.println("======================================================================");

        DatabaseConfig dbConfig = new DatabaseConfig();
        UniGramHttpServer server = new UniGramHttpServer(0, dbConfig);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            server.start();
            String baseUrl = server.getBaseUrl();
            System.out.println("Server running on " + baseUrl + " (DB configured: " + dbConfig.isConfigured() + ")");

            // 1. Register Student A (Seller)
            System.out.println("\n[Journey 1] Register Student A (Seller)");
            String studentAPayload = "{\"name\":\"Aman Sharma\",\"email\":\"aman0101.becse25@chitkara.edu.in\",\"studentId\":\"2110990101\",\"password\":\"Student@123\"}";
            HttpRequest regAReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(studentAPayload)).build();
            HttpResponse<String> regARes = client.send(regAReq, HttpResponse.BodyHandlers.ofString());
            check("Student A registration returns 201 Created", regARes.statusCode() == 201, regARes.body());
            String tokenA = extractJsonField(regARes.body(), "token");
            check("Student A receives valid Bearer session token", tokenA != null && !tokenA.isEmpty(), tokenA);

            // 2. Register Student B (Buyer)
            System.out.println("\n[Journey 2] Register Student B (Buyer)");
            String studentBPayload = "{\"name\":\"Bhavya Patel\",\"email\":\"bhavya0202.becse25@chitkara.edu.in\",\"studentId\":\"2110990202\",\"password\":\"Student@123\"}";
            HttpRequest regBReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(studentBPayload)).build();
            HttpResponse<String> regBRes = client.send(regBReq, HttpResponse.BodyHandlers.ofString());
            check("Student B registration returns 201 Created", regBRes.statusCode() == 201, regBRes.body());
            String tokenB = extractJsonField(regBRes.body(), "token");
            check("Student B receives valid Bearer session token", tokenB != null && !tokenB.isEmpty(), tokenB);

            // 3. Student A creates a listing
            System.out.println("\n[Journey 3] Student A creates a marketplace listing");
            String listingPayload = "{\"title\":\"Operating Systems Concepts (Silberschatz)\",\"categoryId\":\"books-notes\",\"price\":550.00,\"originalPrice\":1200.00,\"itemCondition\":\"Like New\",\"meetupSpot\":\"Picasso Block\",\"description\":\"Complete OS book with dinosaur cover.\"}";
            HttpRequest createListingReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings"))
                    .header("Authorization", "Bearer " + tokenA)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(listingPayload)).build();
            HttpResponse<String> createListingRes = client.send(createListingReq, HttpResponse.BodyHandlers.ofString());
            check("Listing creation succeeds (201 Created)", createListingRes.statusCode() == 201, createListingRes.body());
            check("Listing begins in pending state for moderation", createListingRes.body().contains("\"status\":\"pending\""), createListingRes.body());
            long listingId = extractJsonLong(createListingRes.body(), "id");
            check("Assigned valid listing ID", listingId > 0, "ID: " + listingId);

            // 4. Admin logs in and approves listing
            System.out.println("\n[Journey 4] Admin logs in and approves listing");
            HttpRequest adminLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"admin@chitkara.edu.in\",\"password\":\"Unimart@123\"}")).build();
            HttpResponse<String> adminLoginRes = client.send(adminLoginReq, HttpResponse.BodyHandlers.ofString());
            check("Admin logs in successfully (200 OK)", adminLoginRes.statusCode() == 200, adminLoginRes.body());
            String adminToken = extractJsonField(adminLoginRes.body(), "token");

            HttpRequest approveReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings/" + listingId + "/approve"))
                    .header("Authorization", "Bearer " + adminToken)
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> approveRes = client.send(approveReq, HttpResponse.BodyHandlers.ofString());
            check("Admin approves listing (200 OK)", approveRes.statusCode() == 200, approveRes.body());
            check("Listing transitions to approved", approveRes.body().contains("\"status\":\"approved\""), approveRes.body());

            // 5. Student B adds listing to Wishlist
            System.out.println("\n[Journey 5] Student B adds listing to Wishlist");
            HttpRequest addWishlistReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/wishlist"))
                    .header("Authorization", "Bearer " + tokenB)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"listingId\":" + listingId + "}")).build();
            HttpResponse<String> addWishlistRes = client.send(addWishlistReq, HttpResponse.BodyHandlers.ofString());
            check("Add to wishlist returns 200 OK", addWishlistRes.statusCode() == 200, addWishlistRes.body());

            HttpRequest getWishlistReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/wishlist"))
                    .header("Authorization", "Bearer " + tokenB)
                    .GET().build();
            HttpResponse<String> getWishlistRes = client.send(getWishlistReq, HttpResponse.BodyHandlers.ofString());
            check("Student B wishlist contains listing ID", getWishlistRes.body().contains(String.valueOf(listingId)), getWishlistRes.body());

            // 6. Student B initiates conversation with Student A
            System.out.println("\n[Journey 6] Student B initiates conversation and message exchange");
            // Resolve Student A user id
            HttpRequest meAReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .header("Authorization", "Bearer " + tokenA).GET().build();
            HttpResponse<String> meARes = client.send(meAReq, HttpResponse.BodyHandlers.ofString());
            String studentAId = extractJsonField(meARes.body(), "userId");

            HttpRequest meBReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .header("Authorization", "Bearer " + tokenB).GET().build();
            HttpResponse<String> meBRes = client.send(meBReq, HttpResponse.BodyHandlers.ofString());
            String studentBId = extractJsonField(meBRes.body(), "userId");

            String convPayload = String.format("{\"buyerId\":\"%s\",\"sellerId\":\"%s\",\"productId\":\"%d\"}",
                    studentBId, studentAId, listingId);
            HttpRequest createConvReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Authorization", "Bearer " + tokenB)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(convPayload)).build();
            HttpResponse<String> createConvRes = client.send(createConvReq, HttpResponse.BodyHandlers.ofString());
            check("Conversation creation returns 201 Created", createConvRes.statusCode() == 201, createConvRes.body());
            String convId = extractJsonField(createConvRes.body(), "conversationId");
            check("Conversation ID generated", convId != null && !convId.isEmpty(), convId);

            // Student B sends message
            HttpRequest msg1Req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + convId + "/messages"))
                    .header("Authorization", "Bearer " + tokenB)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"Hello Aman! Can we meetup at Picasso Block at 3pm?\"}")).build();
            HttpResponse<String> msg1Res = client.send(msg1Req, HttpResponse.BodyHandlers.ofString());
            check("Student B sends message (201 Created)", msg1Res.statusCode() == 201, msg1Res.body());

            // Student A sends reply
            HttpRequest msg2Req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + convId + "/messages"))
                    .header("Authorization", "Bearer " + tokenA)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"Sure Bhavya, 3pm at Picasso block works great!\"}")).build();
            HttpResponse<String> msg2Res = client.send(msg2Req, HttpResponse.BodyHandlers.ofString());
            check("Student A replies (201 Created)", msg2Res.statusCode() == 201, msg2Res.body());

            // 7. Verify Message History
            System.out.println("\n[Journey 7] Verify chronological message history and participant authorization");
            HttpRequest getMsgsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + convId + "/messages"))
                    .header("Authorization", "Bearer " + tokenA)
                    .GET().build();
            HttpResponse<String> getMsgsRes = client.send(getMsgsReq, HttpResponse.BodyHandlers.ofString());
            check("Message history retrieved successfully (200 OK)", getMsgsRes.statusCode() == 200, getMsgsRes.body());
            check("Contains message 1 from Student B", getMsgsRes.body().contains("Picasso Block at 3pm"), getMsgsRes.body());
            check("Contains message 2 from Student A", getMsgsRes.body().contains("works great"), getMsgsRes.body());

            // 8. Server Restart State Retention Simulation
            System.out.println("\n[Journey 8] Server Restart State Retention Simulation");
            com.unigram.collection.InMemoryStorage sharedStorage = server.getRouter().getInMemoryStorage();
            server.stop(0);

            // Re-instantiate and start new server instance pointing to same storage/DB
            server = new UniGramHttpServer(0, dbConfig, sharedStorage);
            server.start();
            baseUrl = server.getBaseUrl();
            System.out.println("Restarted server running on " + baseUrl);

            HttpRequest postRestartListingsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings/" + listingId))
                    .GET().build();
            HttpResponse<String> postRestartListingsRes = client.send(postRestartListingsReq, HttpResponse.BodyHandlers.ofString());
            check("Listing survives server restart (200 OK)", postRestartListingsRes.statusCode() == 200, postRestartListingsRes.body());
            check("Listing title matches pre-restart value", postRestartListingsRes.body().contains("Operating Systems Concepts"), postRestartListingsRes.body());

            HttpRequest postRestartConvReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + convId))
                    .header("X-Dev-User-Id", studentAId)
                    .GET().build();
            HttpResponse<String> postRestartConvRes = client.send(postRestartConvReq, HttpResponse.BodyHandlers.ofString());
            check("Conversation survives server restart (200 OK)", postRestartConvRes.statusCode() == 200, postRestartConvRes.body());

            // 9. Outage Simulation: Durable Write Rejection
            System.out.println("\n[Journey 9] Outage Simulation: Durable Write Rejection");
            HttpRequest outageReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings"))
                    .header("Authorization", "Bearer " + tokenA)
                    .header("Content-Type", "application/json")
                    .header("X-Require-Durable", "true")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Fake Item\",\"categoryId\":\"books-notes\",\"price\":100.00}")).build();
            HttpResponse<String> outageRes = client.send(outageReq, HttpResponse.BodyHandlers.ofString());
            check("Server rejects write with 503 Service Unavailable when MySQL is disconnected", outageRes.statusCode() == 503, outageRes.body());
            check("Server response indicates database unavailability", outageRes.body().contains("unavailable"), outageRes.body());

        } catch (Exception e) {
            System.err.println("[!] Exception during verification: " + e.getMessage());
            e.printStackTrace();
            totalFailed++;
        } finally {
            server.stop(0);
        }

        System.out.println("\n======================================================================");
        System.out.printf("End-to-End Summary: Passed: %d | Failed: %d%n", totalPassed, totalFailed);
        System.out.println("======================================================================\n");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static long extractJsonLong(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return -1;
        int start = idx + key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == ' ')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
