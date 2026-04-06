package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    // @Schema(description = "Indicates if the request was successful")
    private boolean success;

    // @Schema(description = "Response message")
    private String message;

    // @Schema(description = "Response data")
    private T data;

    // @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    // @Schema(description = "Machine-readable business error code")
    private String errorCode;

    public ApiResponse(boolean success, String message, T data, LocalDateTime timestamp) {
        this(success, message, data, timestamp, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now(), null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now(), null);
    }

    public static <T> ApiResponse<T> success(T data, LocalDateTime timestamp) {
        return new ApiResponse<>(true, "Success", data, timestamp, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now(), null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "Success", null, LocalDateTime.now(), null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now(), null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now(), null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now(), errorCode);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now(), errorCode);
    }
}
