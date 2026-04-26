package com.hoz.hozitech.application.services.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.constant.RealtimeEventType;
import com.hoz.hozitech.application.repositories.NotificationRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.realtime.RealtimeEventPushService;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Notification;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RealtimeEventPushService realtimeEventPushService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(UUID userId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(notifications.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
                
        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Notification does not belong to user");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void createForUser(UUID userId, NotificationPayload payload) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        Order order = null;
        if (payload.getTargetType() != null
                && "ORDER".equalsIgnoreCase(payload.getTargetType())
                && payload.getTargetId() != null) {
            UUID orderId = parseUuid(payload.getTargetId());
            if (orderId != null) {
                order = orderRepository.findById(orderId).orElse(null);
            }
        }

        Notification notification = Notification.builder()
                .title(payload.getTitle())
                .content(payload.getContent())
                .type(payload.getType())
                .eventCode(payload.getEventCode())
                .targetUrl(payload.getTargetUrl())
                .targetType(payload.getTargetType())
                .targetId(payload.getTargetId())
                .metadataJson(writeMetadata(payload.getMetadata()))
                .user(user)
                .order(order)
                .build();
        Notification saved = notificationRepository.save(notification);
        realtimeEventPushService.sendToUser(userId, RealtimeEventType.USER_NOTIFICATION_CREATED, mapToResponse(saved));
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .isRead(n.getIsRead())
                .type(n.getType())
                .eventCode(n.getEventCode())
                .orderId(n.getOrder() != null ? n.getOrder().getId() : null)
                .targetUrl(n.getTargetUrl())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .metadata(readMetadata(n.getMetadataJson()))
                .createdAt(n.getCreatedAt())
                .build();
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
