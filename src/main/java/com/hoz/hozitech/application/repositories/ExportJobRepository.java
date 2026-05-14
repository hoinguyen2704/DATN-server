package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ExportJob;
import com.hoz.hozitech.domain.enums.ExportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
    List<ExportJob> findByExpiresAtBeforeAndStatusIn(
            LocalDateTime expiresAt,
            Collection<ExportJobStatus> statuses);
}
