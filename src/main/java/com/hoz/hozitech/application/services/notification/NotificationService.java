package com.hoz.hozitech.application.services.notification;

import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface NotificationService {
    
    PageResponse<NotificationResponse> getMyNotifications(UUID userId, int page, int size);
    
    long getUnreadCount(UUID userId);
    
    void markAsRead(UUID userId, UUID notificationId);
    
    void markAllAsRead(UUID userId);
}
