package com.unigram.exception;

/**
 * Common base checked exception for all Uni-Gram domain exceptions.
 * Serves as the root of the application's checked exception hierarchy.
 */
public class UniGramException extends Exception {

    public UniGramException(String message) {
        super(message);
    }

    public UniGramException(String message, Throwable cause) {
        super(message, cause);
    }

    public UniGramException(Throwable cause) {
        super(cause);
    }
}
