package com.hoz.hozitech.config.exceptions;

import java.util.Arrays;

public abstract class LocalizedRuntimeException extends RuntimeException {

    private String messageKey;
    private Object[] messageArgs = new Object[0];

    protected LocalizedRuntimeException(String devMessage) {
        super(devMessage);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return Arrays.copyOf(messageArgs, messageArgs.length);
    }

    @SuppressWarnings("unchecked")
    public <T extends LocalizedRuntimeException> T withMessageKey(String messageKey, Object... messageArgs) {
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : Arrays.copyOf(messageArgs, messageArgs.length);
        return (T) this;
    }
}
