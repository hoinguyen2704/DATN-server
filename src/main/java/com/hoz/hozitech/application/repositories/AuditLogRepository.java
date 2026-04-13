package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:targetType IS NULL OR UPPER(a.targetType) = UPPER(:targetType))
                AND (:targetId IS NULL OR a.targetId = :targetId)
            """)
    Page<AuditLog> search(@Param("targetType") String targetType,
                          @Param("targetId") UUID targetId,
                          Pageable pageable);
}
