package com.unigram;

import com.unigram.api.JsonUtil;
import com.unigram.security.GoogleTokenVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Negative and Positive Test Suite for Google ID Token Cryptographic Verification.
 * Enforces RSA-SHA256 signature, algorithm RS256, audience, issuer, expiry, email_verified,
 * and university domain constraints.
 */
public class GoogleTokenVerifierTest {

    private KeyPair validKeyPair;
    private KeyPair attackerKeyPair;
    private static final String TEST_KID = "test-google-key-id-1";
    private static final String ATTACKER_KID = "attacker-key-id-99";
    private static final String CLIENT_ID = "30099110138-testclient.apps.googleusercontent.com";

    @BeforeEach
    public void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        validKeyPair = kpg.generateKeyPair();
        attackerKeyPair = kpg.generateKeyPair();

        // Register valid public key with GoogleTokenVerifier
        GoogleTokenVerifier.registerTestKey(TEST_KID, validKeyPair.getPublic());
        GoogleTokenVerifier.registerTestKey(ATTACKER_KID, attackerKeyPair.getPublic());
    }

    @AfterEach
    public void tearDown() {
        GoogleTokenVerifier.clearTestKeys();
    }

    private String createSignedJwt(Map<String, Object> header, Map<String, Object> payload, KeyPair keyPair) throws Exception {
        String hJson = JsonUtil.toJson(header);
        String pJson = JsonUtil.toJson(payload);

        String b64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(hJson.getBytes(StandardCharsets.UTF_8));
        String b64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(pJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = b64Header + "." + b64Payload;

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        byte[] signatureBytes = sig.sign();
        String b64Sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        return signingInput + "." + b64Sig;
    }

    @Test
    public void testValidGoogleIdTokenPasses() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "sub", "109823471092384",
                "email", "student.satkar@chitkara.edu.in",
                "email_verified", true,
                "name", "Satkar Singh",
                "exp", now + 3600,
                "iat", now
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertTrue(result.isValid(), "Valid token must pass verification");
        assertNotNull(result.getClaims());
        assertEquals("student.satkar@chitkara.edu.in", result.getClaims().email);
    }

    @Test
    public void testForgedSignatureFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "sub", "109823471092384",
                "email", "student.satkar@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        // Sign with attacker's key while claiming kid is valid Google key
        String forgedJwt = createSignedJwt(header, payload, attackerKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(forgedJwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Forged token signature must fail verification");
        assertTrue(result.getError().contains("signature verification failed") || result.getError().contains("Invalid"));
    }

    @Test
    public void testTamperedClaimsFail() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> originalPayload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "sub", "109823471092384",
                "email", "student.satkar@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        String validJwt = createSignedJwt(header, originalPayload, validKeyPair);
        String[] parts = validJwt.split("\\.");

        // Attacker attempts to change email to administrator in payload while leaving original signature
        Map<String, Object> tamperedPayload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "sub", "109823471092384",
                "email", "admin@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );
        String b64TamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                JsonUtil.toJson(tamperedPayload).getBytes(StandardCharsets.UTF_8));
        String tamperedJwt = parts[0] + "." + b64TamperedPayload + "." + parts[2];

        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(tamperedJwt, CLIENT_ID, true);
        assertFalse(result.isValid(), "Tampered payload claims must fail signature verification");
    }

    @Test
    public void testInsecureAlgorithmNoneFails() {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "none", "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "student@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        String b64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtil.toJson(header).getBytes(StandardCharsets.UTF_8));
        String b64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String unsignedJwt = b64Header + "." + b64Payload + ".";

        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(unsignedJwt, CLIENT_ID, true);
        assertFalse(result.isValid(), "Algorithm 'none' must be strictly rejected");
        assertTrue(result.getError().contains("Unsupported or insecure") || result.getError().contains("RS256"));
    }

    @Test
    public void testMissingKeyIdFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT"); // No kid
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "student@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Missing kid in header must fail");
        assertTrue(result.getError().contains("kid"));
    }

    @Test
    public void testAudienceMismatchFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", "wrong-attacker-client-id.apps.googleusercontent.com",
                "email", "student@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Mismatched audience must fail");
        assertTrue(result.getError().contains("Audience mismatch"));
    }

    @Test
    public void testMissingClientIdInProductionFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "student@chitkara.edu.in",
                "email_verified", true,
                "exp", now + 3600
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        // Pass null or empty expectedAudience with isProduction = true
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, null, true);

        assertFalse(result.isValid(), "Missing client ID in production mode must fail");
        assertTrue(result.getError().contains("GOOGLE_CLIENT_ID"));
    }

    @Test
    public void testExpiredTokenFails() throws Exception {
        long past = (System.currentTimeMillis() / 1000L) - 3600; // 1 hour ago
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "student@chitkara.edu.in",
                "email_verified", true,
                "exp", past
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Expired token must fail");
        assertTrue(result.getError().contains("expired"));
    }

    @Test
    public void testUnverifiedEmailFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "student@chitkara.edu.in",
                "email_verified", false,
                "exp", now + 3600
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Unverified Google email must fail");
        assertTrue(result.getError().contains("not verified"));
    }

    @Test
    public void testNonChitkaraDomainFails() throws Exception {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = Map.of("alg", "RS256", "kid", TEST_KID, "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", "https://accounts.google.com",
                "aud", CLIENT_ID,
                "email", "attacker@gmail.com",
                "email_verified", true,
                "exp", now + 3600
        );

        String jwt = createSignedJwt(header, payload, validKeyPair);
        GoogleTokenVerifier.VerificationResult result = GoogleTokenVerifier.verify(jwt, CLIENT_ID, true);

        assertFalse(result.isValid(), "Non-Chitkara email domain must fail");
        assertTrue(result.getError().contains("Chitkara University"));
    }

    @Test
    public void testMalformedJwtStructureFails() {
        assertFalse(GoogleTokenVerifier.verify("", CLIENT_ID, true).isValid());
        assertFalse(GoogleTokenVerifier.verify("single_part_token", CLIENT_ID, true).isValid());
        assertFalse(GoogleTokenVerifier.verify("part1.part2", CLIENT_ID, true).isValid());
        assertFalse(GoogleTokenVerifier.verify("part1.part2.part3.part4", CLIENT_ID, true).isValid());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Running GoogleTokenVerifierTest directly...");
        GoogleTokenVerifierTest test = new GoogleTokenVerifierTest();

        int passed = 0;
        int failed = 0;

        java.lang.reflect.Method[] methods = GoogleTokenVerifierTest.class.getDeclaredMethods();
        for (java.lang.reflect.Method m : methods) {
            if (m.isAnnotationPresent(Test.class)) {
                test.setUp();
                try {
                    m.invoke(test);
                    System.out.println("  [PASS] " + m.getName());
                    passed++;
                } catch (Exception e) {
                    System.err.println("  [FAIL] " + m.getName() + ": " + e.getCause().getMessage());
                    failed++;
                } finally {
                    test.tearDown();
                }
            }
        }

        System.out.println("\nGoogleTokenVerifierTest Results: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }
}
