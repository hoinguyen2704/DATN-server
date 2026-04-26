package com.hoz.hozitech.application.services.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.constant.RealtimeEventType;
import com.hoz.hozitech.application.repositories.AdminNotificationReadRepository;
import com.hoz.hozitech.application.repositories.AdminNotificationRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.realtime.RealtimeEventPushService;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.AdminNotification;
import com.hoz.hozitech.domain.entities.AdminNotificationRead;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;
    private final AdminNotificationReadRepository adminNotificationReadRepository;
    private final UserRepository userRepository;
    private final RealtimeEventPushService realtimeEventPushService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID adminUserId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<AdminNotification> notifications = adminNotificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<UUID> notificationIds = notifications.stream().map(AdminNotification::getId).toList();
        Set<UUID> readIds = notificationIds.isEmpty()
                ? Set.of()
                : adminNotificationReadRepository.findReadNotificationIds(adminUserId, notificationIds);
        return PageResponse.of(notifications.map(notification -> mapToResponse(notification, readIds.contains(notification.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID adminUserId) {
        long total = adminNotificationRepository.count();
        long readCount = adminNotificationReadRepository.countByUserId(adminUserId);
        return Math.max(0, total - readCount);
    }

    @Override
    @Transactional
    public void markAsRead(UUID adminUserId, UUID notificationId) {
        AdminNotification notification = adminNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Admin notification not found"));
        if (adminNotificationReadRepository.existsByUserIdAndNotificationId(adminUserId, notificationId)) {
            return;
        }
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new UnauthorizedException("Admin user not found"));
        adminNotificationReadRepository.save(AdminNotificationRead.builder()
                .user(adminUser)
                .notification(notification)
                .build());
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID adminUserId) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new UnauthorizedException("Admin user not found"));
        List<AdminNotification> unreadNotifications = adminNotificationRepository.findUnreadByUserId(adminUserId);
        for (AdminNotification notification : unreadNotifications) {
            adminNotificationReadRepository.save(AdminNotificationRead.builder()
                    .user(adminUser)
                    .notification(notification)
                    .build());
        }
    }

    @Override
    @Transactional
    public void createShared(NotificationPayload payload, boolean markCurrentAdminAsRead) {
        UUID currentAdminId = markCurrentAdminAsRead ? resolveCurrentAdminUserId() : null;
        Map<String, Object> metadata = payload.getMetadata();
        if (currentAdminId != null) {
            metadata = metadata == null
                    ? new java.util.LinkedHashMap<>()
                    : new java.util.LinkedHashMap<>(metadata);
            metadata.put("actorUserId", currentAdminId.toString());
        }

        AdminNotification notification = adminNotificationRepository.save(AdminNotification.builder()
                .title(payload.getTitle())
                .content(payload.getContent())
                .type(payload.getType())
                .eventCode(payload.getEventCode())
                .targetUrl(payload.getTargetUrl())
                .targetType(payload.getTargetType())
                .targetId(payload.getTargetId())
                .metadataJson(writeMetadata(metadata))
                .build());

        if (currentAdminId != null && !adminNotificationReadRepository.existsByUserIdAndNotificationId(currentAdminId, notification.getId())) {
            userRepository.findById(currentAdminId).ifPresent(user ->
                    adminNotificationReadRepository.save(AdminNotificationRead.builder()
                            .user(user)
                            .notification(notification)
                            .build()));
        }

        realtimeEventPushService.sendToAdmins(RealtimeEventType.ADMIN_NOTIFICATION_CREATED, mapToResponse(notification, false));
    }

    private UUID resolveCurrentAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        User user = userDetails.getUser();
        if (user == null || user.getRole() == null || user.getRole().getId() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getId().name())) {
            return null;
        }
        return user.getId();
    }

    private NotificationResponse mapToResponse(AdminNotification notification, boolean isRead) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(isRead)
                .type(notification.getType())
                .eventCode(notification.getEventCode())
                .targetUrl(notification.getTargetUrl())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .metadata(readMetadata(notification.getMetadataJson()))
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (IOException ex) {
            return null;
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (IOException ex) {
            return null;
        }
    }
}
