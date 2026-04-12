package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {

    private UUID id;
    private String returnNumber;

    private UUID orderId;
    private String orderNumber;

    private UUID userId;
    private String userName;
    private String userEmail;

    private String status;
    private String refundStatus;

    private String reason;
    private String evidenceNote;
    private String adminNote;

    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private BigDecimal refundAmount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    private List<ReturnItemData> items;
    private List<RefundTransactionData> refunds;
    private List<ReturnStatusHistoryData> statusHistories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemData {
        private UUID id;
        private UUID orderItemId;
        private String productName;
        private String variantName;
        private BigDecimal unitPrice;
        private Integer requestedQuantity;
        private Integer approvedQuantity;
        private BigDecimal lineAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundTransactionData {
        private UUID id;
        private String idempotencyKey;
        private String provider;
        private String transactionId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String failureReason;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnStatusHistoryData {
        private UUID id;
        private String status;
        private String description;
        private LocalDateTime createdAt;
    }
}
