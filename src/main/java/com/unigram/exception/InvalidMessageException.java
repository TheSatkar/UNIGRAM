package com.unigram.exception;

/**
 * Thrown when a message fails validation constraints (e.g., empty content,
 * missing sender, or invalid conversation association).
 */
public class InvalidMessageException extends UniGramException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
