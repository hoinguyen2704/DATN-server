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
public class FeedbackResponse {
    private UUID id;
    private Integer rating;
    private String content;
    private String imagesJson;
    private String status;
    private LocalDateTime createdAt;
    
    private UUID productId;
    private String productName;
    
    private UUID userId;
    private String userName;
    private String userAvatar;
    
    private UUID orderId;
}
