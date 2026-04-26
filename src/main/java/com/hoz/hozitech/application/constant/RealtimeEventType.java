package com.hoz.hozitech.application.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Không cho phép tạo instance
public final class RealtimeEventType {

    public static final String SUPPORT_TICKET_CREATED = "SUPPORT_TICKET_CREATED";
    public static final String SUPPORT_MESSAGE_CREATED = "SUPPORT_MESSAGE_CREATED";
    public static final String SUPPORT_STATUS_UPDATED = "SUPPORT_STATUS_UPDATED";
    public static final String USER_NOTIFICATION_CREATED = "USER_NOTIFICATION_CREATED";
    public static final String ADMIN_NOTIFICATION_CREATED = "ADMIN_NOTIFICATION_CREATED";
}
