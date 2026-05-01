package com.hoz.hozitech.web.exceptions;

import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when required configuration (environment variables, properties) is missing or invalid.
 * Mapped to HTTP 500 Internal Server Error.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ConfigurationException extends LocalizedRuntimeException {

    private static final String DEFAULT_MESSAGE_KEY = "error.configuration";

    public ConfigurationException(String message) {
        super(message);
        withMessageKey(DEFAULT_MESSAGE_KEY);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        withMessageKey(DEFAULT_MESSAGE_KEY);
    }
}
