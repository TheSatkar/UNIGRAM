package com.unigram.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory session and password recovery OTP manager.
 * Manages cryptographically random bearer tokens and ephemeral single-use OTPs with bounded expiration.
 */
public class SessionManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long SESSION_EXPIRATION_HOURS = 24;
    private static final long OTP_EXPIRATION_MINUTES = 10;
    private static final long RESET_TOKEN_EXPIRATION_MINUTES = 15;
    private static final int MAX_OTP_ATTEMPTS = 3;

    public static class UserSession {
        private final String token;
        private final String userId;
        private final String name;
        private final String email;
        private final String role;
        private final String studentId;
        private final Instant createdAt;
        private final Instant expiresAt;

        public UserSession(String token, String userId, String name, String email, String role, String studentId, Instant createdAt, Instant expiresAt) {
            this.token = token;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.studentId = studentId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStudentId() { return studentId; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    public static class OtpState {
        private final String email;
        private final String code;
        private final Instant expiresAt;
        private int attempts;
        private boolean consumed;
        private String resetToken;
        private Instant resetTokenExpiresAt;

        public OtpState(String email, String code, Instant expiresAt) {
            this.email = email;
            this.code = code;
            this.expiresAt = expiresAt;
            this.attempts = 0;
            this.consumed = false;
        }

        public String getEmail() { return email; }
        public String getCode() { return code; }
        public Instant getExpiresAt() { return expiresAt; }
        public int getAttempts() { return attempts; }
        public boolean isConsumed() { return consumed; }
        public String getResetToken() { return resetToken; }
        public Instant getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    }

    // Active sessions: token -> UserSession
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    // Active OTP state: email (lowercase) -> OtpState
    private final Map<String, OtpState> activeOtps = new ConcurrentHashMap<>();

    // Reset tokens: resetToken -> email
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    /**
     * Creates a new authenticated session for a user.
     */
    public UserSession createSession(String userId, String name, String email, String role, String studentId) {
        String token = generateSecureToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_EXPIRATION_HOURS, ChronoUnit.HOURS);

        UserSession session = new UserSession(token, userId, name, email, role, studentId, now, expiresAt);
        sessions.put(token, session);
        return session;
    }

    /**
     * Resolves an active, non-expired session by token.
     */
    public Optional<UserSession> validateSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        UserSession session = sessions.get(token.trim());
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * Invalidates a session (logout).
     */
    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token.trim());
        }
    }

    /**
     * Invalidates all active sessions for a specific user ID (e.g. after password reset).
     */
    public void invalidateAllUserSessions(String userId) {
        if (userId == null) return;
        sessions.entrySet().removeIf(entry -> userId.equals(entry.getValue().getUserId()));
    }

    /**
     * Generates a 6-digit numeric OTP for password recovery.
     */
    public String createPasswordResetOtp(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        int randomNum = 100000 + SECURE_RANDOM.nextInt(900000);
        String code = String.valueOf(randomNum);

        Instant expiresAt = Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES);
        OtpState state = new OtpState(normalizedEmail, code, expiresAt);
        activeOtps.put(normalizedEmail, state);
        return code;
    }

    /**
     * Verifies an OTP code for an email and returns a single-use password reset token if valid.
     */
    public synchronized Optional<String> verifyOtp(String email, String candidateCode) {
        String normalizedEmail = email.toLowerCase().trim();
        OtpState state = activeOtps.get(normalizedEmail);
        if (state == null) {
            return Optional.empty();
        }

        if (state.consumed) {
            return Optional.empty();
        }

        if (Instant.now().isAfter(state.expiresAt)) {
            activeOtps.remove(normalizedEmail);
            return Optional.empty();
        }

        state.attempts++;
        if (state.attempts > MAX_OTP_ATTEMPTS) {
            activeOtps.remove(normalizedEmail);
            return Optional.empty();
        }

        if (!state.code.equals(candidateCode.trim())) {
            return Optional.empty();
        }

        // Code matched! Generate single-use reset token and mark OTP consumed
        state.consumed = true;
        String resetToken = generateSecureToken();
        state.resetToken = resetToken;
        state.resetTokenExpiresAt = Instant.now().plus(RESET_TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES);
        resetTokens.put(resetToken, normalizedEmail);

        return Optional.of(resetToken);
    }

    /**
     * Validates and consumes a single-use reset token, returning the associated email.
     */
    public synchronized Optional<String> consumeResetToken(String resetToken) {
        if (resetToken == null || resetToken.trim().isEmpty()) {
            return Optional.empty();
        }
        String cleanToken = resetToken.trim();
        String email = resetTokens.remove(cleanToken);
        if (email == null) {
            return Optional.empty();
        }

        OtpState state = activeOtps.get(email);
        if (state == null || state.resetTokenExpiresAt == null || Instant.now().isAfter(state.resetTokenExpiresAt)) {
            activeOtps.remove(email);
            return Optional.empty();
        }

        activeOtps.remove(email);
        return Optional.of(email);
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
