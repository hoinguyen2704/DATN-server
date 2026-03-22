package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class NotFoundException extends RuntimeException {
    private HttpStatus status;
    private String userMessage;
    private String devMessage;

    public NotFoundException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.NOT_FOUND;
        this.devMessage = devMessage;
    }

    public NotFoundException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.NOT_FOUND;
        this.userMessage = userMessage;
        this.devMessage = devMessage;
    }
}
