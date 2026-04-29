package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConflictException extends LocalizedRuntimeException {
    private final HttpStatus status;
    private final String userMessage;

    public ConflictException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.CONFLICT;
        this.userMessage = null;
    }

    public ConflictException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.CONFLICT;
        this.userMessage = userMessage;
    }

    public String getDevMessage() {
        return getMessage();
    }
}
