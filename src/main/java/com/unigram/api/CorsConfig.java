package com.unigram.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cross-Origin Resource Sharing (CORS) handler for the Uni-Gram HTTP API.
 * Configures permitted origins for local React development (Vite @ 5173, CRA @ 3000)
 * and handles OPTIONS preflight negotiations safely without wildcards.
 */
public class CorsConfig {

    public static final String ENV_CORS_ORIGINS = "UNIGRAM_CORS_ORIGINS";
    public static final String DEFAULT_ORIGINS = "http://localhost:5173,http://localhost:3000";

    public static final String ALLOWED_METHODS = "GET, POST, OPTIONS";
    public static final String ALLOWED_HEADERS = "Content-Type, X-Dev-User-Id";
    public static final String MAX_AGE_SECONDS = "86400";

    private final Set<String> allowedOrigins;

    public CorsConfig() {
        this(resolveConfiguredOrigins());
    }

    public CorsConfig(Set<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            this.allowedOrigins = Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList("http://localhost:5173", "http://localhost:3000")));
        } else {
            this.allowedOrigins = Collections.unmodifiableSet(new HashSet<>(allowedOrigins));
        }
    }

    private static Set<String> resolveConfiguredOrigins() {
        String env = System.getenv(ENV_CORS_ORIGINS);
        if (env == null || env.trim().isEmpty()) {
            env = DEFAULT_ORIGINS;
        }
        Set<String> origins = new HashSet<>();
        for (String origin : env.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                origins.add(trimmed);
            }
        }
        return origins;
    }

    public boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }
        return allowedOrigins.contains(origin.trim());
    }

    /**
     * Applies standard CORS headers to an outgoing response if the Origin is allowed.
     *
     * @param exchange active HTTP exchange
     */
    public void applyCorsHeaders(HttpExchange exchange) {
        List<String> originHeaders = exchange.getRequestHeaders().get("Origin");
        if (originHeaders != null && !originHeaders.isEmpty()) {
            String origin = originHeaders.get(0).trim();
            if (isAllowedOrigin(origin)) {
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Access-Control-Allow-Origin", origin);
                responseHeaders.set("Access-Control-Allow-Methods", ALLOWED_METHODS);
                responseHeaders.set("Access-Control-Allow-Headers", ALLOWED_HEADERS);
                responseHeaders.set("Access-Control-Max-Age", MAX_AGE_SECONDS);
                responseHeaders.set("Vary", "Origin");
            }
        }
    }

    /**
     * Handles an HTTP OPTIONS preflight request.
     * If the method is OPTIONS, applies headers and terminates exchange with 204 No Content.
     *
     * @param exchange active HTTP exchange
     * @return true if handled (preflight), false if not an OPTIONS request
     * @throws IOException if sending response fails
     */
    public boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            applyCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }
}
