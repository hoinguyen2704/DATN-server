package com.hoz.hozitech.domain.dtos.response;

import com.hoz.hozitech.domain.entities.ExportJob;
import com.hoz.hozitech.domain.enums.ExportJobStatus;
import com.hoz.hozitech.domain.enums.ExportJobType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExportJobResponse {
    private UUID jobId;
    private ExportJobType type;
    private ExportJobStatus status;
    private Long totalRows;
    private Long processedRows;
    private Integer progress;
    private String fileName;
    private String contentType;
    private String errorMessage;
    private Boolean downloadable;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    public static ExportJobResponse from(ExportJob job) {
        boolean downloadable = ExportJobStatus.SUCCEEDED == job.getStatus()
                && job.getFilePath() != null
                && !job.getFilePath().isBlank();
        UUID jobId = job.getId();
        return ExportJobResponse.builder()
                .jobId(jobId)
                .type(job.getType())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .progress(job.getProgress())
                .fileName(job.getFileName())
                .contentType(job.getContentType())
                .errorMessage(job.getErrorMessage())
                .downloadable(downloadable)
                .downloadUrl(downloadable && jobId != null
                        ? "/admin/api/v1/export-jobs/" + jobId + "/download"
                        : null)
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .expiresAt(job.getExpiresAt())
                .build();
    }
}
