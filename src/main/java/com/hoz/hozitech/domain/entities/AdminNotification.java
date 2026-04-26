package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_notifications", indexes = {
        @Index(name = "idx_admin_notification_created_at", columnList = "created_at"),
        @Index(name = "idx_admin_notification_type", columnList = "type")
})
public class AdminNotification extends AbstractAuditingEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "event_code", length = 80)
    private String eventCode;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "target_type", length = 80)
    private String targetType;

    @Column(name = "target_id", length = 120)
    private String targetId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
