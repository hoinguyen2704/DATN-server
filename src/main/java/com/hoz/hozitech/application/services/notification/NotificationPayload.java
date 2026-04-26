package com.hoz.hozitech.application.services.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {
    private String type;
    private String eventCode;
    private String title;
    private String content;
    private String targetUrl;
    private String targetType;
    private String targetId;
    private Map<String, Object> metadata;
}
