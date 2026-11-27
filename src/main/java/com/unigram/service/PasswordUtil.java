package com.unigram.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for hashing and verifying student passwords using salted SHA-256.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = sha256Base64(salt, rawPassword);
        return saltBase64 + ":" + hashBase64;
    }

    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        String[] parts = storedHash.split(":", 2);
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String expectedHash = parts[1];
            String actualHash = sha256Base64(salt, rawPassword);
            return MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    actualHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String sha256Base64(byte[] salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable.", e);
        }
    }
}
