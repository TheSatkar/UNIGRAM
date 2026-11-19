package com.unigram.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON serialization and deserialization utility using Google Gson.
 * Configured with ISO-8601 LocalDateTime adapters, Unicode/emoji preservation,
 * and request body size enforcement.
 */
public final class JsonUtil {

    public static final int DEFAULT_MAX_BODY_BYTES = 64 * 1024; // 64 KB limit

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src != null ? src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    json != null && !json.getAsString().isEmpty()
                            ? LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null)
            .disableHtmlEscaping() // Keep characters like quotes, &, <, > without unnecessary escaping
            .create();

    private JsonUtil() {
        // Prevent instantiation of utility class
    }

    public static Gson getGson() {
        return GSON;
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonSyntaxException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }

    /**
     * Reads the HTTP request body enforcing a maximum byte limit (default 64 KB).
     *
     * @param exchange  the active HttpExchange
     * @param maxBytes  maximum permissible payload size in bytes
     * @return the request body decoded as UTF-8 String
     * @throws PayloadTooLargeException if body exceeds maxBytes
     * @throws IOException              if reading fails
     */
    public static String readRequestBody(HttpExchange exchange, int maxBytes) throws IOException, PayloadTooLargeException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int totalBytes = 0;
        int read;

        while ((read = in.read(chunk)) != -1) {
            totalBytes += read;
            if (totalBytes > maxBytes) {
                throw new PayloadTooLargeException("Request body exceeds maximum allowed size of " + maxBytes + " bytes (64 KB).");
            }
            buffer.write(chunk, 0, read);
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }

    public static class PayloadTooLargeException extends Exception {
        public PayloadTooLargeException(String message) {
            super(message);
        }
    }
}
