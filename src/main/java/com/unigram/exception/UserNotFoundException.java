package com.unigram.exception;

/**
 * Thrown when an operation references a user that does not exist in the system.
 */
public class UserNotFoundException extends UniGramException {

    private final String userId;

    public UserNotFoundException(String userId) {
        super("User not found with ID: " + userId);
        this.userId = userId;
    }

    public UserNotFoundException(String userId, String message) {
        super(message);
        this.userId = userId;
    }

    public UserNotFoundException(String userId, String message, Throwable cause) {
        super(message, cause);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
