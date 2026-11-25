package com.unigram.exception;

/**
 * Base checked exception for Uni-Gram domain errors.
 */
public class UniGramException extends Exception {

    public UniGramException(String message) {
        super(message);
    }

    public UniGramException(String message, Throwable cause) {
        super(message, cause);
    }
}
