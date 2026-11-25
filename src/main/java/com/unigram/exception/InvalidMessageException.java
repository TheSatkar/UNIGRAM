package com.unigram.exception;

/**
 * Thrown when a message fails validation (such as empty or blank text).
 */
public class InvalidMessageException extends UniGramException {

    public InvalidMessageException(String message) {
        super(message);
    }
}
