package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_social_user_provider", columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "idx_social_user", columnList = "user_id"),
                @Index(name = "idx_social_provider", columnList = "provider")
        })
public class UserSocialAccount extends AbstractAuditingEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 150)
    private String providerEmail;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}
