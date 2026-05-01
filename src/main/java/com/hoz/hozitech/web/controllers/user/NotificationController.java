package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.NotificationResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestAPI("${api.prefix-client}/notifications")
@Authenticated
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        
        return ResponseEntity.ok(responseFactory.success("response.notification.list_fetched",
                notificationService.getMyNotifications(userDetails.getUser().getId(), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        long count = notificationService.getUnreadCount(userDetails.getUser().getId());
        return ResponseEntity.ok(responseFactory.success("response.notification.unread_count_fetched",
                Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        
        notificationService.markAsRead(userDetails.getUser().getId(), id);
        return ResponseEntity.ok(responseFactory.success("response.notification.marked_read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        notificationService.markAllAsRead(userDetails.getUser().getId());
        return ResponseEntity.ok(responseFactory.success("response.notification.all_marked_read"));
    }
}
