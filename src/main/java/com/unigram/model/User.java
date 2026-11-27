package com.unigram.model;

import java.util.Objects;

/**
 * Represents a registered student in the Uni-Gram messaging system.
 */
public class User {

    private final String userId;
    private String fullName;
    private String email;
    private String passwordHash;

    public User(String userId, String fullName, String email, String passwordHash) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        this.userId = userId.trim();
        this.fullName = fullName.trim();
        this.email = email.trim();
        this.passwordHash = passwordHash != null ? passwordHash.trim() : "";
    }

    public User(String userId, String fullName, String email) {
        this(userId, fullName, email, "");
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        this.email = email.trim();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash != null ? passwordHash.trim() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{userId='" + userId + "', fullName='" + fullName + "', email='" + email + "'}";
    }
}
