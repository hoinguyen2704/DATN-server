package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class InvalidParamException extends RuntimeException {
    private HttpStatus status;
    private String userMessage;
    private String devMessage;

    public InvalidParamException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.BAD_REQUEST;
        this.devMessage = devMessage;
    }

    public InvalidParamException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.BAD_REQUEST;
        this.userMessage = userMessage;
        this.devMessage = devMessage;
    }
}
