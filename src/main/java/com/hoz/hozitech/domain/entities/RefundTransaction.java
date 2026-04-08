package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import com.hoz.hozitech.domain.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refund_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_idempotency", columnNames = "idempotency_key")
}, indexes = {
        @Index(name = "idx_refund_return_request", columnList = "return_request_id"),
        @Index(name = "idx_refund_order", columnList = "order_id"),
        @Index(name = "idx_refund_status", columnList = "status")
})
public class RefundTransaction extends AbstractAuditingEntity {

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
