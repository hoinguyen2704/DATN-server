package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String title;
    private String content;
    private Boolean isRead;
    private String type;
    private UUID orderId;
    private LocalDateTime createdAt;
}
