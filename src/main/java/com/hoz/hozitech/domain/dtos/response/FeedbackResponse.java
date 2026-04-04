package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import com.hoz.hozitech.domain.enums.FeedbackStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackResponse {
    private UUID id;
    private Integer rating;
    private String content;
    private String imagesJson;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    
    private UUID productId;
    private String productName;
    private UUID variantId;
    private String variantName;
    private UUID userId;
    private String userName;
    private String userAvatar;
    
    private UUID orderId;

    private String adminReply;
    private LocalDateTime repliedAt;
    
    private Integer editCount;
}
