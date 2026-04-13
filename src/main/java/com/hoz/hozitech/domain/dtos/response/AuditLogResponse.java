package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID actorUserId;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String targetType;
    private UUID targetId;
    private String oldValue;
    private String newValue;
    private String reason;
    private LocalDateTime createdAt;
}
