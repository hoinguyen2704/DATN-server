package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ConflictException extends RuntimeException {
    private HttpStatus status;
    private String userMessage;
    private String devMessage;

    public ConflictException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.CONFLICT;
        this.devMessage = devMessage;
    }

    public ConflictException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.CONFLICT;
        this.userMessage = userMessage;
        this.devMessage = devMessage;
    }
}
