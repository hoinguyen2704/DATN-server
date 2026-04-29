package com.hoz.hozitech.web.exceptions;

import java.util.HashMap;
import java.util.Map;

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
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(resolveMessage(ex, ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage())));
    }

    @ExceptionHandler(InvalidParamException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParamException(InvalidParamException ex) {
        log.warn("InvalidParamException: {}", ex.getDevMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(resolveMessage(ex, ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage())));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("UnauthorizedException: {}", ex.getDevMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(resolveMessage(ex, ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage())));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getDevMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(resolveMessage(ex, ex.getUserMessage() != null ? ex.getUserMessage() : ex.getDevMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business error: ", ex);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(localizationUtils.localizeErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("BusinessException code={} message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getErrorCode().name(), resolveMessage(ex, ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = localizationUtils.localizeValidationMessage(error.getDefaultMessage());
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation error: {}", errors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.validation_failed"), errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatch parameter={} value={}", ex.getName(), ex.getValue());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage(
                        "error.method_argument_type_mismatch",
                        ex.getName(),
                        ex.getValue())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.access_denied")));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.bad_credentials")));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(resolveMessage(ex, ex.getMessage())));
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ApiResponse<Void>> handleExportException(ExportException ex) {
        log.error("Export failed: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.export_failed")));
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConfigurationException(ConfigurationException ex) {
        log.error("Configuration error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.configuration")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.data_integrity_violation")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(localizationUtils.getLocalizedMessage("error.internal_server_error")));
    }

    private String resolveMessage(LocalizedRuntimeException ex, String fallbackMessage) {
        String messageKey = ex.getMessageKey() != null ? ex.getMessageKey() : fallbackMessage;
        String localized = localizationUtils.getLocalizedMessageOrDefault(messageKey, fallbackMessage, ex.getMessageArgs());
        if (localized == null || localized.equals(fallbackMessage)) {
            return localizationUtils.localizeErrorMessage(fallbackMessage);
        }
        return localized;
    }
}
