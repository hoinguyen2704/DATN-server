package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "otp_tokens", indexes = {
        @Index(name = "idx_otp_email", columnList = "email")
})
public class OtpToken extends AbstractAuditingEntity {

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
}
