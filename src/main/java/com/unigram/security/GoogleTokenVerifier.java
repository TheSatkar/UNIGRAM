package com.unigram.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unigram.api.JsonUtil;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates Google OAuth2 / OpenID Connect ID Tokens (JWT).
 * 
 * Enforces cryptographic RSA-SHA256 signature verification against Google's
 * published JWKS signing keys (https://www.googleapis.com/oauth2/v3/certs),
 * algorithm RS256, issuer, audience, expiration, email verification,
 * and Chitkara University domain constraints.
 */
public class GoogleTokenVerifier {

    public static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    public static final String ISSUER_SECURE = "https://accounts.google.com";
    public static final String ISSUER_COMPAT = "accounts.google.com";
    public static final String PERMITTED_DOMAIN = "@chitkara.edu.in";

    private static final Map<String, PublicKey> KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PublicKey> TEST_KEYS = new ConcurrentHashMap<>();
    private static volatile long lastJwksFetchMillis = 0;
    private static final long CACHE_TTL_MILLIS = 3600_000L; // 1 hour

    public static class GoogleClaims {
        public String iss;
        public String aud;
        public String sub;
        public String email;
        public Boolean email_verified;
        public String name;
        public String picture;
        public String hd;
        public Long exp;
        public Long iat;
    }

    public static class VerificationResult {
        private final boolean valid;
        private final String error;
        private final GoogleClaims claims;

        private VerificationResult(boolean valid, String error, GoogleClaims claims) {
            this.valid = valid;
            this.error = error;
            this.claims = claims;
        }

        public static VerificationResult success(GoogleClaims claims) {
            return new VerificationResult(true, null, claims);
        }

        public static VerificationResult failure(String error) {
            return new VerificationResult(false, error, null);
        }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
        public GoogleClaims getClaims() { return claims; }
    }

    /**
     * Registers an explicit public key for testing (e.g. in test suites).
     */
    public static void registerTestKey(String kid, PublicKey publicKey) {
        if (kid != null && publicKey != null) {
            TEST_KEYS.put(kid, publicKey);
        }
    }

    /**
     * Clears test keys.
     */
    public static void clearTestKeys() {
        TEST_KEYS.clear();
    }

    /**
     * Verifies a raw Google JWT ID token string.
     *
     * @param idToken raw 3-part JWT string
     * @param expectedAudience expected client ID (GOOGLE_CLIENT_ID)
     * @param isProduction whether production constraints are enforced
     * @return VerificationResult containing success or detailed failure reason
     */
    public static VerificationResult verify(String idToken, String expectedAudience, boolean isProduction) {
        if (idToken == null || idToken.trim().isEmpty()) {
            return VerificationResult.failure("ID token is missing or empty.");
        }

        // In production mode, GOOGLE_CLIENT_ID is strictly required
        if (isProduction && (expectedAudience == null || expectedAudience.trim().isEmpty())) {
            return VerificationResult.failure("GOOGLE_CLIENT_ID environment variable is strictly required in production mode.");
        }

        String[] parts = idToken.trim().split("\\.", -1);
        if (parts.length != 3) {
            return VerificationResult.failure("Invalid JWT format: Token must contain exactly header, payload, and signature.");
        }

        // 1. Decode & Verify Header
        String headerJson;
        String alg;
        String kid;
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            headerJson = new String(headerBytes, StandardCharsets.UTF_8);
            JsonObject headerObj = JsonUtil.fromJson(headerJson, JsonObject.class);
            if (headerObj == null) {
                return VerificationResult.failure("Malformed JWT header.");
            }
            JsonElement algElem = headerObj.get("alg");
            alg = algElem != null ? algElem.getAsString() : null;
            JsonElement kidElem = headerObj.get("kid");
            kid = kidElem != null ? kidElem.getAsString() : null;
        } catch (Exception e) {
            return VerificationResult.failure("Failed to parse JWT header: " + e.getMessage());
        }

        // Enforce RS256 algorithm (reject none, HS256, etc.)
        if (!"RS256".equalsIgnoreCase(alg)) {
            return VerificationResult.failure("Unsupported or insecure JWT algorithm: '" + alg + "'. RS256 is strictly required.");
        }

        if (kid == null || kid.trim().isEmpty()) {
            return VerificationResult.failure("Missing key ID ('kid') in JWT header.");
        }

        // 2. Resolve Public Key
        PublicKey publicKey = resolvePublicKey(kid.trim());
        if (publicKey == null) {
            return VerificationResult.failure("Cryptographic verification failed: No matching Google signing key found for kid '" + kid + "'.");
        }

        // 3. Cryptographic RSA-SHA256 Signature Verification
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            byte[] signedContent = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            verifier.update(signedContent);

            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
            if (!verifier.verify(signatureBytes)) {
                return VerificationResult.failure("Cryptographic signature verification failed: Invalid or forged token signature.");
            }
        } catch (Exception e) {
            return VerificationResult.failure("Cryptographic signature verification error: " + e.getMessage());
        }

        // 4. Decode & Verify Payload Claims
        GoogleClaims claims;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            claims = JsonUtil.fromJson(payloadJson, GoogleClaims.class);
        } catch (Exception e) {
            return VerificationResult.failure("Malformed JWT payload: " + e.getMessage());
        }

        if (claims == null) {
            return VerificationResult.failure("Failed to parse token claims.");
        }

        // 5. Verify Issuer
        if (claims.iss == null || (!claims.iss.equals(ISSUER_SECURE) && !claims.iss.equals(ISSUER_COMPAT))) {
            return VerificationResult.failure("Invalid token issuer: '" + claims.iss + "'. Must be Google Accounts.");
        }

        // 6. Verify Audience
        if (expectedAudience != null && !expectedAudience.trim().isEmpty()) {
            if (claims.aud == null || !claims.aud.equals(expectedAudience.trim())) {
                return VerificationResult.failure("Audience mismatch: Token audience '" + claims.aud +
                        "' does not match configured Google Client ID.");
            }
        } else if (isProduction) {
            return VerificationResult.failure("Audience check failed: Google Client ID is unconfigured.");
        }

        // 7. Verify Expiration (with 30-second clock skew tolerance)
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (claims.exp == null || claims.exp < (nowSeconds - 30)) {
            return VerificationResult.failure("Token has expired (exp: " + claims.exp + ", current: " + nowSeconds + ").");
        }

        // 8. Verify Email Verification Flag
        if (claims.email_verified == null || !claims.email_verified) {
            return VerificationResult.failure("Google email is not verified.");
        }

        // 9. Verify University Domain Constraint
        if (claims.email == null || !claims.email.toLowerCase().endsWith(PERMITTED_DOMAIN)) {
            return VerificationResult.failure("Access restricted: Only official Chitkara University accounts (" +
                    PERMITTED_DOMAIN + ") may authenticate.");
        }

        return VerificationResult.success(claims);
    }

    public static VerificationResult verify(String idToken, String expectedAudience) {
        return verify(idToken, expectedAudience, false);
    }

    private static PublicKey resolvePublicKey(String kid) {
        // 1. Check test key override
        if (TEST_KEYS.containsKey(kid)) {
            return TEST_KEYS.get(kid);
        }

        // 2. Check cache
        if (KEY_CACHE.containsKey(kid)) {
            return KEY_CACHE.get(kid);
        }

        // 3. Refresh cache from Google JWKS if expired or missing key
        long now = System.currentTimeMillis();
        if (now - lastJwksFetchMillis > 60_000L) { // Limit network fetch frequency to at most once per minute
            refreshJwksKeys();
        }

        return KEY_CACHE.get(kid);
    }

    private static synchronized void refreshJwksKeys() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_JWKS_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parseAndCacheJwks(response.body());
                lastJwksFetchMillis = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.err.println("[GoogleTokenVerifier] Warning: Unable to refresh Google JWKS keys: " + e.getMessage());
        }
    }

    public static void parseAndCacheJwks(String jwksJson) {
        try {
            JsonObject root = JsonUtil.fromJson(jwksJson, JsonObject.class);
            if (root == null || !root.has("keys")) return;

            JsonArray keys = root.getAsJsonArray("keys");
            KeyFactory kf = KeyFactory.getInstance("RSA");

            for (JsonElement elem : keys) {
                JsonObject kObj = elem.getAsJsonObject();
                String kty = kObj.has("kty") ? kObj.get("kty").getAsString() : null;
                if (!"RSA".equalsIgnoreCase(kty)) continue;

                String kid = kObj.has("kid") ? kObj.get("kid").getAsString() : null;
                String n = kObj.has("n") ? kObj.get("n").getAsString() : null;
                String e = kObj.has("e") ? kObj.get("e").getAsString() : null;

                if (kid != null && n != null && e != null) {
                    byte[] nBytes = Base64.getUrlDecoder().decode(n);
                    byte[] eBytes = Base64.getUrlDecoder().decode(e);

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
                    PublicKey pubKey = kf.generatePublic(spec);
                    KEY_CACHE.put(kid, pubKey);
                }
            }
        } catch (Exception ex) {
            System.err.println("[GoogleTokenVerifier] Failed to parse JWKS: " + ex.getMessage());
        }
    }
}
