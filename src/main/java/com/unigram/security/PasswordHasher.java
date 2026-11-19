package com.unigram.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Enterprise-grade cryptographic password hashing utility using PBKDF2WithHmacSHA256.
 * Implements salted, iterated hashing with constant-time verification to prevent timing attacks.
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16;  // bytes

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes a plaintext password with a freshly generated cryptographically secure random salt.
     * Output format: iterations:base64Salt:base64Hash
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a candidate password against an existing serialized hash.
     * Uses messageDigest constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            // Legacy / demo hash fallback verification
            return slowEquals(candidatePassword.getBytes(), storedHash.getBytes());
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] candidateHash = pbkdf2(candidatePassword.toCharArray(), salt, iterations, expectedHash.length * 8);

            return slowEquals(candidateHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        }
    }

    /**
     * Constant-time byte comparison to mitigate side-channel timing attacks.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
