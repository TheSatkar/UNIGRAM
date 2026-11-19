package com.unigram.api;

/**
 * Health check data payload reporting server readiness and database connectivity.
 */
public class HealthResponse {

    private final String status;
    private final String database;
    private final String persistenceStatus;
    private final String apiVersion;

    public HealthResponse(String status, String database, String apiVersion) {
        this.status = status;
        this.database = database;
        this.persistenceStatus = "CONNECTED".equalsIgnoreCase(database) ? "MYSQL_DURABLE" : "DEMO_EPHEMERAL";
        this.apiVersion = apiVersion;
    }

    public String getStatus() {
        return status;
    }

    public String getDatabase() {
        return database;
    }

    public String getPersistenceStatus() {
        return persistenceStatus;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
