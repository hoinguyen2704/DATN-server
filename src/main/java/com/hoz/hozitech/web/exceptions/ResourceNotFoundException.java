package com.hoz.hozitech.web.exceptions;

import com.hoz.hozitech.config.exceptions.LocalizedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (entity) cannot be found.
 * Mapped to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends LocalizedRuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
        withMessageKey("error.resource_not_found." + toMessageSegment(resource), id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    private static String toMessageSegment(String value) {
        return value == null
                ? "resource"
                : value.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
