package com.hoz.hozitech.application.services.audit;

import com.hoz.hozitech.domain.dtos.response.AuditLogResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.User;

import java.util.UUID;

public interface AuditLogService {
    void record(User actor,
                String action,
                String targetType,
                UUID targetId,
                String oldValue,
                String newValue,
                String reason);

    PageResponse<AuditLogResponse> getAuditLogs(String targetType,
                                                UUID targetId,
                                                int page,
                                                int size,
                                                String sortBy,
                                                String sortDir);
}
