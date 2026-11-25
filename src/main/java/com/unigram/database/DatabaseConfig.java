package com.unigram.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Loads MySQL connection settings from the local .env file, Java system
 * properties, or environment variables without hardcoding credentials.
 */
public class DatabaseConfig {

    public static final String ENV_DB_URL = "UNIGRAM_DB_URL";
    public static final String ENV_DB_USER = "UNIGRAM_DB_USER";
    public static final String ENV_DB_PASSWORD = "UNIGRAM_DB_PASSWORD";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        this(
            resolveConfig(ENV_DB_URL),
            resolveConfig(ENV_DB_USER),
            resolveConfig(ENV_DB_PASSWORD)
        );
    }

    public DatabaseConfig(String url, String user, String password) {
        this.url = url != null ? url.trim() : null;
        this.user = user != null ? user.trim() : null;
        this.password = password;
    }

    private static String resolveConfig(String key) {
        String envFileValue = readFromEnvFile(key);
        if (envFileValue != null && !envFileValue.trim().isEmpty()) {
            return envFileValue.trim();
        }

        String propValue = System.getProperty(key);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }

        String sysValue = System.getenv(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        return null;
    }

    private static String readFromEnvFile(String key) {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                if (k.equals(key)) {
                    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    return v;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isConfigured() {
        return url != null && !url.isEmpty()
                && user != null && !user.isEmpty()
                && password != null;
    }

    public void validate() throws DatabaseException {
        if (url == null || url.isEmpty()) {
            throw new DatabaseException("Missing configuration: " + ENV_DB_URL + " is not set.");
        }
        if (user == null || user.isEmpty()) {
            throw new DatabaseException("Missing configuration: " + ENV_DB_USER + " is not set.");
        }
        if (password == null) {
            throw new DatabaseException("Missing configuration: " + ENV_DB_PASSWORD + " is not set.");
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
