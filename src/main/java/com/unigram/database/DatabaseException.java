package com.unigram.database;

/**
 * Custom exception representing failures during database access, connectivity, or SQL execution.
 * Wraps low-level SQLExceptions while ensuring passwords and sensitive credentials are not leaked.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
