package com.unigram.api;

/**
 * Request payload for synchronizing a student user reference from UniMart into Uni-Gram.
 * Supports flexible field names (userId/id, fullName/name, email) for seamless React integration.
 */
public class SyncUserRequest {

    private String userId;
    private String id;
    private String fullName;
    private String name;
    private String email;

    public SyncUserRequest() {
    }

    public SyncUserRequest(String userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUserId() {
        if (userId != null && !userId.trim().isEmpty()) {
            return userId.trim();
        }
        return id != null ? id.trim() : null;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        return name != null ? name.trim() : null;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email.trim() : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
