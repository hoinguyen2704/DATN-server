package com.hoz.hozitech.web.exceptions;

import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an export operation (Excel, PDF, etc.) fails.
 * Mapped to HTTP 500 Internal Server Error.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExportException extends LocalizedRuntimeException {

    private static final String DEFAULT_MESSAGE_KEY = "error.export_failed";

    public ExportException(String message) {
        super(message);
        withMessageKey(DEFAULT_MESSAGE_KEY);
    }

    public ExportException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        withMessageKey(DEFAULT_MESSAGE_KEY);
    }
}
