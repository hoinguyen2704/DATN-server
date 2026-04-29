package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotFoundException extends LocalizedRuntimeException {
    private final HttpStatus status;
    private final String userMessage;

    public NotFoundException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.NOT_FOUND;
        this.userMessage = null;
    }

    public NotFoundException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.NOT_FOUND;
        this.userMessage = userMessage;
    }

    public String getDevMessage() {
        return getMessage();
    }
}
