package com.unigram.database;

/**
 * Encapsulates MySQL database connection parameters for Uni-Gram.
 * 
 * Sources configuration strictly from Environment Variables:
 * - UNIGRAM_DB_URL      (e.g., jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC)
 * - UNIGRAM_DB_USER     (e.g., unigram_app)
 * - UNIGRAM_DB_PASSWORD (database account password)
 * 
 * Never hardcodes or commits credentials to version control.
 */
public class DatabaseConfig {

    public static final String ENV_DB_URL = "UNIGRAM_DB_URL";
    public static final String ENV_DB_USER = "UNIGRAM_DB_USER";
    public static final String ENV_DB_PASSWORD = "UNIGRAM_DB_PASSWORD";

    private final String url;
    private final String user;
    private final String password;

    /**
     * Loads database configuration from system environment variables or local .env file.
     */
    public DatabaseConfig() {
        this(
            resolveConfig(ENV_DB_URL),
            resolveConfig(ENV_DB_USER),
            resolveConfig(ENV_DB_PASSWORD)
        );
    }

    private static String resolveConfig(String envKey) {
        // Priority 1: Check .env file so existing environment variables do not override it
        String envFileVal = readFromEnvFile(envKey);
        if (envFileVal != null && !envFileVal.trim().isEmpty()) {
            return envFileVal.trim();
        }

        // Priority 2: System properties (-Dkey=value)
        String propVal = System.getProperty(envKey);
        if (propVal != null && !propVal.trim().isEmpty()) {
            return propVal.trim();
        }

        // Priority 3: System environment variables
        String sysEnvVal = System.getenv(envKey);
        if (sysEnvVal != null && !sysEnvVal.trim().isEmpty()) {
            return sysEnvVal.trim();
        }

        return null;
    }

    private static String readFromEnvFile(String envKey) {
        java.io.File[] candidates = new java.io.File[] {
            new java.io.File(".env"),
            new java.io.File(System.getProperty("user.dir", "."), ".env"),
            new java.io.File(System.getProperty("user.dir", "."), "../.env")
        };
        for (java.io.File envFile : candidates) {
            if (envFile.exists() && envFile.isFile()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("#") || !line.contains("=")) continue;
                        int idx = line.indexOf('=');
                        String k = line.substring(0, idx).trim();
                        String v = line.substring(idx + 1).trim();
                        if (k.equals(envKey)) {
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            return v;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Constructor allowing programmatic or testing injection.
     */
    public DatabaseConfig(String url, String user, String password) {
        this.url = url != null ? url.trim() : null;
        this.user = user != null ? user.trim() : null;
        this.password = password != null ? password : null;
    }

    /**
     * Checks whether all mandatory environment variables are populated.
     */
    public boolean isConfigured() {
        return url != null && !url.isEmpty() &&
               user != null && !user.isEmpty() &&
               password != null;
    }

    /**
     * Validates that all required configuration variables are present.
     *
     * @throws DatabaseException if configuration is incomplete
     */
    public void validate() throws DatabaseException {
        if (url == null || url.isEmpty()) {
            throw new DatabaseException("Missing database configuration: Environment variable '" 
                    + ENV_DB_URL + "' is not set.");
        }
        if (user == null || user.isEmpty()) {
            throw new DatabaseException("Missing database configuration: Environment variable '" 
                    + ENV_DB_USER + "' is not set.");
        }
        if (password == null) {
            throw new DatabaseException("Missing database configuration: Environment variable '" 
                    + ENV_DB_PASSWORD + "' is not set.");
        }
    }

    public String getUrl() {
        return url;
    }

    public String getJdbcUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Masked toString to prevent credential leakage in logs or terminal output.
     */
    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "url='" + (url != null ? url : "[NOT SET]") + '\'' +
                ", user='" + (user != null ? user : "[NOT SET]") + '\'' +
                ", password=" + (password != null ? "********" : "[NOT SET]") +
                ", isConfigured=" + isConfigured() +
                '}';
    }
}
