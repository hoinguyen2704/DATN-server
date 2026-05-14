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
    private String productSlug;
    private String productName;
    private UUID variantId;
    private String variantSku;
    private String variantName;
    private UUID userId;
    private String userName;
    private String userAvatar;
    
    private UUID orderId;
    private String orderNumber;

    private String adminReply;
    private LocalDateTime repliedAt;
    
    private Integer editCount;

    public FeedbackResponse(
            UUID id,
            Integer rating,
            String content,
            String imagesJson,
            FeedbackStatus status,
            LocalDateTime createdAt,
            UUID productId,
            String productSlug,
            String productName,
            UUID userId,
            String userName,
            String userAvatar,
            String adminReply,
            LocalDateTime repliedAt,
            Integer editCount) {
        this.id = id;
        this.rating = rating;
        this.content = content;
        this.imagesJson = imagesJson;
        this.status = status;
        this.createdAt = createdAt;
        this.productId = productId;
        this.productSlug = productSlug;
        this.productName = productName;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.adminReply = adminReply;
        this.repliedAt = repliedAt;
        this.editCount = editCount;
    }
}
