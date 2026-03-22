package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class UnauthorizedException extends RuntimeException {
    private HttpStatus status;
    private String userMessage;
    private String devMessage;

    public UnauthorizedException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.UNAUTHORIZED;
        this.devMessage = devMessage;
    }

    public UnauthorizedException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.UNAUTHORIZED;
        this.userMessage = userMessage;
        this.devMessage = devMessage;
    }
}
