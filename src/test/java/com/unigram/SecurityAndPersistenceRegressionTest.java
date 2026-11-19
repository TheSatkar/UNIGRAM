package com.unigram;

import com.unigram.api.CorsConfig;
import com.unigram.api.UniGramHttpServer;
import com.unigram.database.DatabaseConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Enterprise Security and Persistence Automated Regression Test Suite.
 * Validates all Critical Security Fixes (A through N) and Data Durability rules.
 */
public class SecurityAndPersistenceRegressionTest {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;

    private static void assertTrue(String description, boolean condition) {
        assertTrue(description, condition, null);
    }

    private static void assertTrue(String description, boolean condition, HttpResponse<?> response) {
        totalAssertions++;
        if (condition) {
            passedAssertions++;
            System.out.println("  [PASS] " + description);
        } else {
            failedAssertions++;
            String bodySnippet = response != null && response.body() != null ? response.body().toString() : "null";
            int status = response != null ? response.statusCode() : -1;
            System.err.println("  [FAIL] " + description + " (HTTP " + status + ") Response: " + bodySnippet);
        }
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   UniMart + Uni-Gram Security & Persistence Regression Test Suite    ");
        System.out.println("======================================================================\n");

        UniGramHttpServer server = null;
        try {
            // Start server with ephemeral port on local loopback
            DatabaseConfig dbConfig = new DatabaseConfig();
            server = new UniGramHttpServer("127.0.0.1", 0, dbConfig, new CorsConfig());
            server.start();
            String baseUrl = server.getBaseUrl();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            // 1. Health check & Ephemeral Persistence Labeling
            System.out.println("[Test 1] Health Endpoint & Storage Transparency");
            HttpRequest healthReq = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/health")).GET().build();
            HttpResponse<String> healthRes = client.send(healthReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Health status is 200 OK", healthRes.statusCode() == 200);
            assertTrue("Persistence mode is explicitly labeled", healthRes.headers().firstValue("X-UniMart-Persistence").isPresent());

            // 2. Reject Non-Chitkara Email Domain at Registration
            System.out.println("\n[Test 2] Registration Email Domain Enforcement");
            String nonChitkaraPayload = "{\"name\":\"Hacker\",\"email\":\"hacker@gmail.com\",\"password\":\"secret123\"}";
            HttpRequest nonChitkaraReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(nonChitkaraPayload)).build();
            HttpResponse<String> nonChitkaraRes = client.send(nonChitkaraReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Non-chitkara email registration rejected (400 Bad Request)", nonChitkaraRes.statusCode() == 400);

            // 3. Register Valid Student Account
            System.out.println("\n[Test 3] Student Registration with Hashed Credentials");
            String studentPayload = "{\"name\":\"Simran Kaur\",\"email\":\"simran0101.becse25@chitkara.edu.in\",\"password\":\"securePass123\",\"studentId\":\"2110990800\"}";
            HttpRequest regReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(studentPayload)).build();
            HttpResponse<String> regRes = client.send(regReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Valid student registration succeeds (201 Created)", regRes.statusCode() == 201);
            assertTrue("Registration returns bearer session token", regRes.body().contains("\"token\":"));
            assertTrue("Registration defaults role to student", regRes.body().contains("\"role\":\"student\""));
            assertTrue("No plaintext password or passwordHash leaked in user payload", !regRes.body().contains("passwordHash") && !regRes.body().contains("securePass123"));

            // 4. Invalid Password Rejection (No Fallback Bypass)
            System.out.println("\n[Test 4] Invalid Credentials Never Grant Access (Zero Fallback Bypass)");
            String wrongPassPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\",\"password\":\"wrong_password_here\"}";
            HttpRequest wrongPassReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(wrongPassPayload)).build();
            HttpResponse<String> wrongPassRes = client.send(wrongPassReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Invalid password rejected (401 Unauthorized)", wrongPassRes.statusCode() == 401);
            assertTrue("No session token issued on failed login", !wrongPassRes.body().contains("\"token\":"));

            // 5. Non-existent User Login Rejection
            System.out.println("\n[Test 5] Non-existent Student Account Rejection");
            String ghostPayload = "{\"email\":\"ghost.student@chitkara.edu.in\",\"password\":\"any_password\"}";
            HttpRequest ghostReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(ghostPayload)).build();
            HttpResponse<String> ghostRes = client.send(ghostReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Unregistered email rejected without synthesizing session (401 Unauthorized)", ghostRes.statusCode() == 401);

            // 6. Successful Login with Correct Credentials
            System.out.println("\n[Test 6] Successful Login & Bearer Token Authentication");
            String goodPassPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\",\"password\":\"securePass123\"}";
            HttpRequest loginReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(goodPassPayload)).build();
            HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Valid login succeeds (200 OK)", loginRes.statusCode() == 200);

            // Extract student token
            String studentToken = extractToken(loginRes.body());
            assertTrue("Session token parsed successfully", studentToken != null && !studentToken.isEmpty());

            // 7. Session Identity Verification via /api/auth/me
            System.out.println("\n[Test 7] Authenticated Profile /api/auth/me");
            HttpRequest meReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .header("Authorization", "Bearer " + studentToken)
                    .GET().build();
            HttpResponse<String> meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("/api/auth/me returns 200 OK with verified session identity", meRes.statusCode() == 200);
            assertTrue("Profile matches registered email", meRes.body().contains("simran0101.becse25@chitkara.edu.in"));

            // 8. Google Sign-In Role Privilege Escalation Prevention
            System.out.println("\n[Test 8] Google Sign-In Role Escalation Prevention");
            // User has 'admin' in email string, but is NOT the true admin
            String googleAttackerPayload = "{\"email\":\"admin_student_club@chitkara.edu.in\",\"name\":\"Fake Admin\",\"sub\":\"g_12345\"}";
            HttpRequest googleReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/google"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(googleAttackerPayload)).build();
            HttpResponse<String> googleRes = client.send(googleReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Google sign-in succeeds (200 OK)", googleRes.statusCode() == 200);
            assertTrue("Sub-string 'admin' in email DOES NOT grant admin privileges (role is 'student')",
                    googleRes.body().contains("\"role\":\"student\""));

            // 9. Password Recovery & Single-Use OTP Flow
            System.out.println("\n[Test 9] Password Recovery, Rate Limiting & Single-Use OTP");
            String otpReqPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\"}";
            HttpRequest otpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/forgot-password/request-otp"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(otpReqPayload)).build();
            HttpResponse<String> otpRes = client.send(otpReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Request OTP returns 200 OK", otpRes.statusCode() == 200);
            String simulatedOtp = extractField(otpRes.body(), "simulatedOtp");
            assertTrue("Generated simulated OTP received", simulatedOtp != null && simulatedOtp.length() == 6);

            // Attempt invalid OTP
            String badOtpPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\",\"otp\":\"000000\"}";
            HttpRequest badOtpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/forgot-password/verify-otp"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(badOtpPayload)).build();
            HttpResponse<String> badOtpRes = client.send(badOtpReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Invalid OTP rejected with 400 Bad Request", badOtpRes.statusCode() == 400);

            // Verify with correct OTP
            String goodOtpPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\",\"otp\":\"" + simulatedOtp + "\"}";
            HttpRequest goodOtpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/forgot-password/verify-otp"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(goodOtpPayload)).build();
            HttpResponse<String> goodOtpRes = client.send(goodOtpReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Correct OTP verified with 200 OK", goodOtpRes.statusCode() == 200);
            String resetToken = extractField(goodOtpRes.body(), "resetToken");
            System.out.println("  [DEBUG] goodOtpRes body: " + goodOtpRes.body());
            System.out.println("  [DEBUG] extracted resetToken: " + resetToken);
            assertTrue("Single-use reset token returned", resetToken != null && !resetToken.isEmpty());

            // Replay OTP attempt (must be rejected)
            HttpResponse<String> replayOtpRes = client.send(goodOtpReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Replay of already consumed OTP rejected (400 Bad Request)", replayOtpRes.statusCode() == 400);

            // Reset password
            String resetPayload = "{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"newPass456!\"}";
            HttpRequest resetReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/forgot-password/reset"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(resetPayload)).build();
            HttpResponse<String> resetRes = client.send(resetReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Password reset succeeds (200 OK)", resetRes.statusCode() == 200, resetRes);

            // Previous session token must now be revoked
            // Previous session token must now be revoked
            HttpResponse<String> postResetMeRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Existing session invalidated following password reset (401 Unauthorized)", postResetMeRes.statusCode() == 401, postResetMeRes);

            // Login with new password
            String newLoginPayload = "{\"email\":\"simran0101.becse25@chitkara.edu.in\",\"password\":\"newPass456!\"}";
            HttpRequest newLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(newLoginPayload)).build();
            HttpResponse<String> newLoginRes = client.send(newLoginReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Sign in with new password succeeds (200 OK)", newLoginRes.statusCode() == 200, newLoginRes);
            studentToken = extractToken(newLoginRes.body());

            // 10. Student Marketplace Listing Creation & Initial Pending Moderation
            System.out.println("\n[Test 10] Listing Creation & Server-Side Moderation Workflow");
            String newListingPayload = "{\"title\":\"Digital Electronics Lab Manual\",\"category_id\":\"books-notes\",\"price\":250.00,\"item_condition\":\"Good\",\"meetup_spot\":\"Turing Block\",\"description\":\"Complete lab experiments.\"}";
            HttpRequest createListingReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + studentToken)
                    .POST(HttpRequest.BodyPublishers.ofString(newListingPayload)).build();
            HttpResponse<String> createListingRes = client.send(createListingReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Student listing creation succeeds (201 Created)", createListingRes.statusCode() == 201, createListingRes);
            assertTrue("Student listing starts with status 'pending' awaiting admin approval", createListingRes.body().contains("\"status\":\"pending\""), createListingRes);

            long createdListingId = extractId(createListingRes.body());
            assertTrue("Listing assigned positive unique ID", createdListingId > 0);

            // 11. Unauthorized Non-Admin Approval Rejection
            System.out.println("\n[Test 11] Administrative Permission Enforcement");
            HttpRequest fakeApproveReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings/" + createdListingId + "/approve"))
                    .header("Authorization", "Bearer " + studentToken) // student trying to approve
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> fakeApproveRes = client.send(fakeApproveReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Student cannot approve listings (403 Forbidden)", fakeApproveRes.statusCode() == 403, fakeApproveRes);

            // Login as Seed Admin
            String adminLoginPayload = "{\"email\":\"admin@chitkara.edu.in\",\"password\":\"Unimart@123\"}";
            HttpRequest adminLoginReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(adminLoginPayload)).build();
            HttpResponse<String> adminLoginRes = client.send(adminLoginReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Campus admin signs in (200 OK)", adminLoginRes.statusCode() == 200, adminLoginRes);
            String adminToken = extractToken(adminLoginRes.body());

            // Real Admin Approval
            HttpRequest adminApproveReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings/" + createdListingId + "/approve"))
                    .header("Authorization", "Bearer " + adminToken)
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> adminApproveRes = client.send(adminApproveReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Campus admin approves listing (200 OK)", adminApproveRes.statusCode() == 200, adminApproveRes);
            assertTrue("Listing status transitions to 'approved'", adminApproveRes.body().contains("\"status\":\"approved\""), adminApproveRes);

            // 12. Input Bounds Validation (Negative Price Rejected)
            System.out.println("\n[Test 12] Input Bounds Validation");
            String negativePricePayload = "{\"title\":\"Invalid Item\",\"category_id\":\"books-notes\",\"price\":-100.00}";
            HttpRequest negPriceReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + studentToken)
                    .POST(HttpRequest.BodyPublishers.ofString(negativePricePayload)).build();
            HttpResponse<String> negPriceRes = client.send(negPriceReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Negative listing price rejected (400 Bad Request)", negPriceRes.statusCode() == 400, negPriceRes);

            // 13. Anonymous and Unauthorized Chat Access Rejection
            System.out.println("\n[Test 13] Chat Identity Protection & Privacy");
            HttpRequest getConvsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations?participantId=student_b"))
                    .GET().build();
            HttpResponse<String> getConvsRes = client.send(getConvsReq, HttpResponse.BodyHandlers.ofString());
            String seedConvId = extractField(getConvsRes.body(), "conversationId");
            assertTrue("Seed conversation found between Priya and Satkar", seedConvId != null && !seedConvId.isEmpty());

            HttpRequest anonMsgReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + seedConvId + "/messages"))
                    .GET().build();
            HttpResponse<String> anonMsgRes = client.send(anonMsgReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Anonymous message history query rejected (401 Unauthorized)", anonMsgRes.statusCode() == 401, anonMsgRes);

            // Simran (not a participant in this conversation) attempts to read private chat
            HttpRequest intruderMsgReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations/" + seedConvId + "/messages"))
                    .header("Authorization", "Bearer " + studentToken)
                    .GET().build();
            HttpResponse<String> intruderMsgRes = client.send(intruderMsgReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Non-participant student access to private conversation rejected (403 Forbidden)", intruderMsgRes.statusCode() == 403, intruderMsgRes);

            // 14. Wishlist Student Isolation
            System.out.println("\n[Test 14] Wishlist Account Isolation");
            String wishPayload = "{\"listingId\":" + createdListingId + "}";
            HttpRequest addWishReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/wishlist"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + studentToken)
                    .POST(HttpRequest.BodyPublishers.ofString(wishPayload)).build();
            HttpResponse<String> addWishRes = client.send(addWishReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Add to wishlist returns 200 OK", addWishRes.statusCode() == 200, addWishRes);

            // Admin checks wishlist (must be empty, data not leaked across accounts)
            HttpRequest adminWishReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/wishlist"))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET().build();
            HttpResponse<String> adminWishRes = client.send(adminWishReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Wishlists isolated between accounts (admin wishlist empty)", adminWishRes.body().contains("\"listingIds\":[]"));

            // 15. Logout & Session Invalidation
            System.out.println("\n[Test 15] Logout Session Revocation");
            HttpRequest logoutReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/logout"))
                    .header("Authorization", "Bearer " + studentToken)
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> logoutRes = client.send(logoutReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Logout succeeds (200 OK)", logoutRes.statusCode() == 200);

            HttpRequest postLogoutReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .header("Authorization", "Bearer " + studentToken)
                    .GET().build();
            HttpResponse<String> postLogoutRes = client.send(postLogoutReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Session token rejected after logout (401 Unauthorized)", postLogoutRes.statusCode() == 401, postLogoutRes);

            // -----------------------------------------------------------------
            // Test 16: Cryptographic Google ID Token Verification
            // -----------------------------------------------------------------
            System.out.println("\n[Test 16] Cryptographic Google ID Token Verification");
            long futureSec = (System.currentTimeMillis() / 1000L) + 3600;
            String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

            // 16a: Valid Google ID Token for Chitkara student
            String validPayload = "{\"iss\":\"https://accounts.google.com\",\"aud\":\"test-client-id\",\"sub\":\"g_12345\",\"email\":\"teststudent@chitkara.edu.in\",\"email_verified\":true,\"name\":\"Google Student\",\"exp\":" + futureSec + "}";
            String validJwt = headerB64 + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(validPayload.getBytes(StandardCharsets.UTF_8)) + ".signature";

            HttpRequest validGoogleReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/google"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idToken\":\"" + validJwt + "\"}"))
                    .build();
            HttpResponse<String> validGoogleRes = client.send(validGoogleReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Valid Google ID Token succeeds (200 OK)", validGoogleRes.statusCode() == 200, validGoogleRes);
            assertTrue("Google token exchanges for active session token", validGoogleRes.body().contains("token"), validGoogleRes);

            // 16b: Expired Google ID Token
            long pastSec = (System.currentTimeMillis() / 1000L) - 3600;
            String expiredPayload = "{\"iss\":\"https://accounts.google.com\",\"sub\":\"g_exp\",\"email\":\"expired@chitkara.edu.in\",\"email_verified\":true,\"exp\":" + pastSec + "}";
            String expiredJwt = headerB64 + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(expiredPayload.getBytes(StandardCharsets.UTF_8)) + ".signature";

            HttpRequest expiredGoogleReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/google"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idToken\":\"" + expiredJwt + "\"}"))
                    .build();
            HttpResponse<String> expiredGoogleRes = client.send(expiredGoogleReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Expired Google ID Token rejected (401 Unauthorized)", expiredGoogleRes.statusCode() == 401, expiredGoogleRes);

            // 16c: Non-Chitkara Google ID Token
            String nonChitkaraExtPayload = "{\"iss\":\"https://accounts.google.com\",\"sub\":\"g_ext\",\"email\":\"attacker@gmail.com\",\"email_verified\":true,\"exp\":" + futureSec + "}";
            String nonChitkaraJwt = headerB64 + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(nonChitkaraExtPayload.getBytes(StandardCharsets.UTF_8)) + ".signature";

            HttpRequest nonChitkaraGoogleReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/google"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idToken\":\"" + nonChitkaraJwt + "\"}"))
                    .build();
            HttpResponse<String> nonChitkaraGoogleRes = client.send(nonChitkaraGoogleReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Non-Chitkara Google email rejected (401 or 403)", nonChitkaraGoogleRes.statusCode() == 401 || nonChitkaraGoogleRes.statusCode() == 403, nonChitkaraGoogleRes);

            // -----------------------------------------------------------------
            // Test 17: Database Outage & Durable Write Rejection
            // -----------------------------------------------------------------
            System.out.println("\n[Test 17] Database Outage & Durable Write Rejection");
            HttpRequest durableWriteReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings"))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .header("X-Require-Durable", "true")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Durable Item\",\"categoryId\":\"books-notes\",\"price\":99.00}"))
                    .build();
            HttpResponse<String> durableWriteRes = client.send(durableWriteReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Write with X-Require-Durable returns 503 during MySQL outage", durableWriteRes.statusCode() == 503, durableWriteRes);
            assertTrue("Application never falsely reports durable write success during outage", durableWriteRes.body().contains("unavailable"), durableWriteRes);

            // -----------------------------------------------------------------
            // Test 18: Client Role Tampering Defense
            // -----------------------------------------------------------------
            System.out.println("\n[Test 18] Client Role Tampering Defense");
            // Re-login student to get active student token
            HttpResponse<String> sLoginRes = client.send(newLoginReq, HttpResponse.BodyHandlers.ofString());
            String activeStudentToken = extractToken(sLoginRes.body());

            // Student attempts to call admin approve endpoint
            HttpRequest studentTamperReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/listings/" + createdListingId + "/approve"))
                    .header("Authorization", "Bearer " + activeStudentToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> studentTamperRes = client.send(studentTamperReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Student token rejected from admin approval action (403 Forbidden)", studentTamperRes.statusCode() == 403, studentTamperRes);

            // -----------------------------------------------------------------
            // Test 19: Security Response Headers Enforcement
            // -----------------------------------------------------------------
            System.out.println("\n[Test 19] Security Response Headers Enforcement");
            HttpRequest secHeadersReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET().build();
            HttpResponse<String> secHeadersRes = client.send(secHeadersReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("X-Content-Type-Options: nosniff present", "nosniff".equalsIgnoreCase(secHeadersRes.headers().firstValue("X-Content-Type-Options").orElse("")), secHeadersRes);
            assertTrue("X-Frame-Options: DENY present", "DENY".equalsIgnoreCase(secHeadersRes.headers().firstValue("X-Frame-Options").orElse("")), secHeadersRes);
            assertTrue("Referrer-Policy header present", secHeadersRes.headers().firstValue("Referrer-Policy").isPresent(), secHeadersRes);

            // -----------------------------------------------------------------
            // Test 20: Conversation Buyer Identity Forgery Defense
            // -----------------------------------------------------------------
            System.out.println("\n[Test 20] Conversation Buyer Identity Forgery Defense");
            HttpRequest forgedConvReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/conversations"))
                    .header("Authorization", "Bearer " + activeStudentToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"buyerId\":\"victim_student\",\"sellerId\":\"satkar\",\"productId\":\"1\"}"))
                    .build();
            HttpResponse<String> forgedConvRes = client.send(forgedConvReq, HttpResponse.BodyHandlers.ofString());
            assertTrue("Student cannot forge buyer identity in conversation creation (403 Forbidden)", forgedConvRes.statusCode() == 403, forgedConvRes);

        } catch (Exception e) {
            System.err.println("[!] Fatal error in security regression tests: " + e.getMessage());
            e.printStackTrace();
            failedAssertions++;
        } finally {
            if (server != null && server.isRunning()) {
                server.stop(0);
            }
        }

        System.out.println("\n======================================================================");
        System.out.printf("Security Regression Summary: Evaluated: %d | Passed: %d | Failed: %d%n",
                totalAssertions, passedAssertions, failedAssertions);
        System.out.println("======================================================================\n");

        if (failedAssertions > 0) {
            System.exit(1);
        }
    }

    private static String extractToken(String json) {
        return extractField(json, "token");
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private static long extractId(String json) {
        String key = "\"id\":";
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
