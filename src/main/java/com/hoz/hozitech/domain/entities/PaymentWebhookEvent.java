package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_webhook_idempotency", columnNames = "idempotency_key")
}, indexes = {
        @Index(name = "idx_payment_webhook_order_number", columnList = "order_number"),
        @Index(name = "idx_payment_webhook_provider", columnList = "provider")
})
public class PaymentWebhookEvent extends AbstractAuditingEntity {

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "response_code", length = 50)
    private String responseCode;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
