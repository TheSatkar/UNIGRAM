package com.unigram.model;

import java.util.Objects;

/**
 * Represents a student user in the Uni-Gram messaging backend.
 * Demonstrates OOP encapsulation, constructor overloading, and identity-based equality.
 */
public class User {

    // Immutable identity field
    private final String userId;

    // Mutable profile fields
    private String fullName;
    private String email;
    private String role = "student";
    private String studentId = "";
    private String passwordHash = "";

    /**
     * Primary constructor initializing all fields with validation.
     *
     * @param userId   unique student user identifier (cannot be null or blank)
     * @param fullName full student name (cannot be null or blank)
     * @param email    student university email address
     */
    public User(String userId, String fullName, String email) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        this.userId = userId.trim();
        this.fullName = fullName.trim();
        this.email = email != null ? email.trim() : "";
    }

    /**
     * Extended constructor with role, studentId, and passwordHash.
     */
    public User(String userId, String fullName, String email, String role, String studentId, String passwordHash) {
        this(userId, fullName, email);
        if (role != null && !role.trim().isEmpty()) {
            this.role = role.trim();
        }
        if (studentId != null) {
            this.studentId = studentId.trim();
        }
        if (passwordHash != null) {
            this.passwordHash = passwordHash.trim();
        }
    }

    /**
     * Overloaded constructor for quick registration when email is optional or provided later.
     * Demonstrates constructor chaining using the 'this' keyword.
     *
     * @param userId   unique student user identifier
     * @param fullName full student name
     */
    public User(String userId, String fullName) {
        this(userId, fullName, "");
    }

    // Getters and controlled Setters

    public String getUserId() {
        return this.userId;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : "";
    }

    public String getRole() {
        return this.role != null ? this.role : "student";
    }

    public void setRole(String role) {
        this.role = role != null && !role.trim().isEmpty() ? role.trim() : "student";
    }

    public String getStudentId() {
        return this.studentId != null ? this.studentId : "";
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId != null ? studentId.trim() : "";
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash != null ? passwordHash.trim() : "";
    }

    // Method Overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(this.userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + this.userId + '\'' +
                ", fullName='" + this.fullName + '\'' +
                ", email='" + this.email + '\'' +
                '}';
    }
}
