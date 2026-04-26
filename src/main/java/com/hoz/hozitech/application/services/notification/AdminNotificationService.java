package com.hoz.hozitech.application.services.notification;

import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface AdminNotificationService {

    PageResponse<NotificationResponse> getNotifications(UUID adminUserId, int page, int size);

    long getUnreadCount(UUID adminUserId);

    void markAsRead(UUID adminUserId, UUID notificationId);

    void markAllAsRead(UUID adminUserId);

    void createShared(NotificationPayload payload, boolean markCurrentAdminAsRead);
}
