package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import com.hoz.hozitech.domain.enums.RefundStatus;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "return_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_return_request_number", columnNames = "return_number"),
        @UniqueConstraint(name = "uk_return_user_idempotency", columnNames = {"user_id", "idempotency_key"})
}, indexes = {
        @Index(name = "idx_return_order", columnList = "order_id"),
        @Index(name = "idx_return_user", columnList = "user_id"),
        @Index(name = "idx_return_status", columnList = "status"),
        @Index(name = "idx_return_number", columnList = "return_number"),
        @Index(name = "idx_return_idempotency", columnList = "idempotency_key")
})
public class ReturnRequest extends AbstractAuditingEntity {

    @Column(name = "return_number", nullable = false, length = 40)
    private String returnNumber;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "evidence_note", length = 1000)
    private String evidenceNote;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "return_request_evidence_images",
            joinColumns = @JoinColumn(name = "return_request_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "image_url", nullable = false, length = 1000)
    private List<String> evidenceImageUrls = new ArrayList<>();

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnRequestStatus status = ReturnRequestStatus.REQUESTED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.PENDING;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<RefundTransaction> refundTransactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ReturnStatusHistory> statusHistories = new ArrayList<>();
}
