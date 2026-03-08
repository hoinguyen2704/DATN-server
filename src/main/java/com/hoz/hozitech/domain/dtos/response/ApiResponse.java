package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
// @Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    // @Schema(description = "Indicates if the request was successful")
    private boolean success;

    // @Schema(description = "Response message")
    private String message;

    // @Schema(description = "Response data")
    private T data;

    // @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, LocalDateTime timestamp) {
        return new ApiResponse<>(true, "Success", data, timestamp);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "Success", null, LocalDateTime.now());
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }
}
