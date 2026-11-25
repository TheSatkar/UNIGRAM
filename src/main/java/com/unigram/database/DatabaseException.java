package com.unigram.database;

/**
 * Checked exception for database configuration, connection, and SQL errors.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
