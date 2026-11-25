package com.unigram.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens short-lived JDBC connections to MySQL and masks passwords in errors.
 */
public class DatabaseConnectionFactory {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static Connection getConnection(DatabaseConfig config) throws DatabaseException {
        if (config == null) {
            throw new DatabaseException("DatabaseConfig cannot be null.");
        }
        config.validate();

        try {
            DriverManager.setLoginTimeout(3);
            String url = config.getUrl();
            if (!url.contains("connectTimeout=")) {
                String separator = url.contains("?") ? "&" : "?";
                url = url + separator + "connectTimeout=3000&socketTimeout=5000";
            }
            return DriverManager.getConnection(url, config.getUser(), config.getPassword());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to connect to MySQL: " + sanitizeMessage(e.getMessage(), config), e);
        }
    }

    public static String sanitizeMessage(String message, DatabaseConfig config) {
        if (message == null) {
            return "Unknown SQL error.";
        }
        if (config != null && config.getPassword() != null && !config.getPassword().isEmpty()) {
            return message.replace(config.getPassword(), "********");
        }
        return message;
    }
}
