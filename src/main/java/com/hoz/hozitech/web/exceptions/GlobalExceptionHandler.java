package com.hoz.hozitech.web.exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;
import com.hoz.hozitech.config.exceptions.NotFoundException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.config.utils.LocalizationUtils;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LocalizationUtils localizationUtils;

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException ex) {
        log.warn("ConflictException: {}", ex.getDevMessage());
        return localizedRuntimeError(
                HttpStatus.CONFLICT,
                ex,
                ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage());
    }

    @ExceptionHandler(InvalidParamException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParamException(InvalidParamException ex) {
        log.warn("InvalidParamException: {}", ex.getDevMessage());
        return localizedRuntimeError(
                HttpStatus.BAD_REQUEST,
                ex,
                ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("UnauthorizedException: {}", ex.getDevMessage());
        return localizedRuntimeError(
                HttpStatus.UNAUTHORIZED,
                ex,
                ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getDevMessage());
        return localizedRuntimeError(
                HttpStatus.NOT_FOUND,
                ex,
                ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business error: ", ex);
        return localizedLiteralError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException code={} message={}", ex.getErrorCode(), ex.getMessage());
        return localizedRuntimeError(ex.getStatus(), ex.getErrorCode().name(), ex, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = resolveValidationMessage(error.getDefaultMessage());
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation error: {}", errors);
        return localizedErrorWithData(HttpStatus.BAD_REQUEST, "error.validation_failed", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatch parameter={} value={}", ex.getName(), ex.getValue());
        return localizedError(
                HttpStatus.BAD_REQUEST,
                "error.method_argument_type_mismatch",
                ex.getName(),
                ex.getValue());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return localizedError(HttpStatus.FORBIDDEN, "error.access_denied");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return localizedError(HttpStatus.UNAUTHORIZED, "error.bad_credentials");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return localizedRuntimeError(HttpStatus.NOT_FOUND, ex, ex.getMessage());
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ApiResponse<Void>> handleExportException(ExportException ex) {
        log.error("Export failed: ", ex);
        return localizedRuntimeError(HttpStatus.INTERNAL_SERVER_ERROR, ex, ex.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConfigurationException(ConfigurationException ex) {
        log.error("Configuration error: {}", ex.getMessage());
        return localizedRuntimeError(HttpStatus.INTERNAL_SERVER_ERROR, ex, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        return localizedError(HttpStatus.CONFLICT, "error.data_integrity_violation");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        if (isClientDisconnect(ex)) {
            log.warn("Client disconnected while streaming response: {}", ex.getMessage());
            return ResponseEntity.noContent().build();
        }
        log.error("Unexpected error: ", ex);
        return localizedError(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal_server_error");
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            if ("ClientAbortException".equals(simpleName)
                    || "AsyncRequestNotUsableException".equals(simpleName)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe") || normalized.contains("connection reset by peer")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ApiResponse<Void>> localizedRuntimeError(
            HttpStatus status,
            LocalizedRuntimeException ex,
            String fallbackMessage) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(resolveMessage(ex, fallbackMessage)));
    }

    private ResponseEntity<ApiResponse<Void>> localizedRuntimeError(
            HttpStatus status,
            String errorCode,
            LocalizedRuntimeException ex,
            String fallbackMessage) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorCode, resolveMessage(ex, fallbackMessage)));
    }

    private ResponseEntity<ApiResponse<Void>> localizedLiteralError(HttpStatus status, String rawMessage) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(resolveLiteralErrorMessage(rawMessage)));
    }

    private ResponseEntity<ApiResponse<Void>> localizedError(HttpStatus status, String messageKey, Object... args) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(resolveMessage(messageKey, args)));
    }

    private <T> ResponseEntity<ApiResponse<T>> localizedErrorWithData(HttpStatus status, String messageKey, T data, Object... args) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(resolveMessage(messageKey, args), data));
    }

    private String resolveMessage(LocalizedRuntimeException ex, String fallbackMessage) {
        return localizationUtils.resolveLocalizedRuntimeMessage(ex, fallbackMessage);
    }

    private String resolveMessage(String messageKey, Object... args) {
        return localizationUtils.getLocalizedMessage(messageKey, args);
    }

    private String resolveLiteralErrorMessage(String rawMessage) {
        return localizationUtils.localizeErrorMessage(rawMessage);
    }

    private String resolveValidationMessage(String rawMessage) {
        return localizationUtils.localizeValidationMessage(rawMessage);
    }
}
