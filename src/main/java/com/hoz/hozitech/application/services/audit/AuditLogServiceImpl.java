package com.hoz.hozitech.application.services.audit;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.AuditLogRepository;
import com.hoz.hozitech.domain.dtos.response.AuditLogResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.AuditLog;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void record(User actor,
                       String action,
                       String targetType,
                       UUID targetId,
                       String oldValue,
                       String newValue,
                       String reason) {
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .actorRole(actor != null && actor.getRole() != null ? actor.getRole().getId().name() : null)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(String targetType,
                                                       UUID targetId,
                                                       int page,
                                                       int size,
                                                       String sortBy,
                                                       String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PaginationConstant.of(page, size, sort);

        Page<AuditLog> logs = auditLogRepository.search(
                targetType == null || targetType.isBlank() ? null : targetType.trim(),
                targetId,
                pageable);

        return PageResponse.of(logs.map(this::mapToResponse));
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
