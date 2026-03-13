package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.NotificationService;
import com.hoz.hozitech.config.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-client}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(ApiResponse.success("Fetch notifications successfully", 
                notificationService.getMyNotifications(userDetails.getUser().getId(), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        long count = notificationService.getUnreadCount(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Fetch unread count successfully", Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        
        notificationService.markAsRead(userDetails.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read successfully"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        notificationService.markAllAsRead(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read successfully"));
    }
}
