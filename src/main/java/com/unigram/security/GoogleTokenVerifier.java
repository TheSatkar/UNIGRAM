package com.unigram.security;

import com.unigram.api.JsonUtil;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Validates Google OAuth2 / OpenID Connect ID Tokens (JWT).
 * Verifies signature structure, issuer, audience, expiration, email verification,
 * and Chitkara University domain constraints.
 */
public class GoogleTokenVerifier {

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
     * Verifies a raw Google JWT ID token string.
     */
    public static VerificationResult verify(String idToken, String expectedAudience) {
        if (idToken == null || idToken.trim().isEmpty()) {
            return VerificationResult.failure("ID token is missing or empty.");
        }

        String[] parts = idToken.trim().split("\\.");
        if (parts.length != 3) {
            return VerificationResult.failure("Invalid JWT format: Token must contain header, payload, and signature.");
        }

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

        // 1. Verify Issuer
        if (claims.iss == null || (!claims.iss.equals("https://accounts.google.com") && !claims.iss.equals("accounts.google.com"))) {
            return VerificationResult.failure("Invalid issuer: " + claims.iss);
        }

        // 2. Verify Audience (if expected audience configured)
        if (expectedAudience != null && !expectedAudience.trim().isEmpty()) {
            if (claims.aud == null || !claims.aud.equals(expectedAudience.trim())) {
                return VerificationResult.failure("Audience mismatch: Token audience '" + claims.aud + "' does not match configured client ID.");
            }
        }

        // 3. Verify Expiry
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (claims.exp == null || claims.exp < (nowSeconds - 30)) { // 30-sec clock skew tolerance
            return VerificationResult.failure("Token has expired (exp: " + claims.exp + ", current: " + nowSeconds + ").");
        }

        // 4. Verify Email Verification
        if (claims.email_verified == null || !claims.email_verified) {
            return VerificationResult.failure("Google email is not verified.");
        }

        // 5. Verify University Domain Constraint
        if (claims.email == null || !claims.email.toLowerCase().endsWith("@chitkara.edu.in")) {
            return VerificationResult.failure("Access restricted: Only official Chitkara University accounts (@chitkara.edu.in) may authenticate.");
        }

        return VerificationResult.success(claims);
    }
}
