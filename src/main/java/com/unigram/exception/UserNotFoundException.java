package com.unigram.exception;

/**
 * Thrown when an operation references a student ID that does not exist.
 */
public class UserNotFoundException extends UniGramException {

    public UserNotFoundException(String userId) {
        super("Student not found with ID: " + userId);
    }
}
