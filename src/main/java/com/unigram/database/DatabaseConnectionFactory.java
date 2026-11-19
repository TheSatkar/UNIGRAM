package com.unigram.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory for creating dedicated, short-lived JDBC Connection instances.
 * 
 * Demonstrates:
 * - DriverManager.getConnection()
 * - JDBC 4.0+ automatic driver discovery and registration via Java ServiceLoader (SPI)
 * - Thread isolation: Connections are never shared between threads
 * - Defensive error handling and credential protection
 */
public class DatabaseConnectionFactory {

    static {
        // Explicit driver class check for viva demonstration
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // In modern JDBC 4.0+, the driver auto-registers via META-INF/services/java.sql.Driver.
            // If the JAR is not on the classpath, this notice explains the resolution.
        }
    }

    /**
     * Creates and opens a new standalone JDBC Connection.
     * Callers must manage the connection lifecycle using try-with-resources.
     *
     * @param config database configuration
     * @return an open JDBC Connection
     * @throws DatabaseException if connection fails or configuration is missing
     */
    public static Connection getConnection(DatabaseConfig config) throws DatabaseException {
        if (config == null) {
            throw new DatabaseException("DatabaseConfig cannot be null.");
        }
        config.validate();

        try {
            return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
        } catch (SQLException e) {
            String sanitizedMessage = sanitizeSqlExceptionMessage(e.getMessage(), config);
            throw new DatabaseException("Failed to establish JDBC Connection: " + sanitizedMessage, e);
        }
    }

    /**
     * Tests whether a connection can be established and is currently valid.
     *
     * @param config         database configuration
     * @param timeoutSeconds timeout in seconds
     * @return true if valid, false otherwise
     */
    public static boolean testConnection(DatabaseConfig config, int timeoutSeconds) {
        if (config == null || !config.isConfigured()) {
            return false;
        }
        try (Connection conn = getConnection(config)) {
            return conn.isValid(timeoutSeconds);
        } catch (Exception e) {
            return false;
        }
    }

    private static String sanitizeSqlExceptionMessage(String msg, DatabaseConfig config) {
        if (msg == null) return "Unknown SQL error.";
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            return msg.replace(config.getPassword(), "********");
        }
        return msg;
    }
}
