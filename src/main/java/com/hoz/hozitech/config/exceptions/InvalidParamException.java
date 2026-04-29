package com.hoz.hozitech.config.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidParamException extends LocalizedRuntimeException {
    private final HttpStatus status;
    private final String userMessage;

    public InvalidParamException(String devMessage) {
        super(devMessage);
        this.status = HttpStatus.BAD_REQUEST;
        this.userMessage = null;
    }

    public InvalidParamException(String userMessage, String devMessage) {
        super(devMessage);
        this.status = HttpStatus.BAD_REQUEST;
        this.userMessage = userMessage;
    }

    public String getDevMessage() {
        return getMessage();
    }
}
