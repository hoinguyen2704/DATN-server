package com.hoz.hozitech.application.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Không cho phép tạo instance
public final class MailTemplate {
    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_CONFIRMED = "order-confirmed";
    public static final String ORDER_SHIPPED = "order-shipped";
    public static final String RETURN_UPDATED = "return-updated";
    public static final String ORDER_CANCELLED = "order-cancelled";
    public static final String FEEDBACK_REQUEST = "feedback-request";
    public static final String OTP_VERIFICATION = "otp-verification";
    public static final String WELCOME = "welcome";
}
