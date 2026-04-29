package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedException extends LocalizedRuntimeException {
    private final HttpStatus status;
    private final String userMessage;

    public UnauthorizedException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.UNAUTHORIZED;
        this.userMessage = null;
    }

    public UnauthorizedException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.UNAUTHORIZED;
        this.userMessage = userMessage;
    }

    public String getDevMessage() {
        return getMessage();
    }
}
