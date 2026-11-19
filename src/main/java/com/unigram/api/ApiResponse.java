package com.unigram.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standard JSON response envelope for all Uni-Gram HTTP API endpoints.
 * Provides a uniform structure for both success and error payloads.
 *
 * @param <T> data payload type
 */
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String error;
    private final String timestamp;

    public ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, data, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
