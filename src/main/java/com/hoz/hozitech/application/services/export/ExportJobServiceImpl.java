package com.hoz.hozitech.application.services.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.repositories.ExportJobRepository;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.domain.dtos.request.ExportJobRequest;
import com.hoz.hozitech.domain.dtos.response.ExportJobResponse;
import com.hoz.hozitech.domain.entities.ExportJob;
import com.hoz.hozitech.domain.enums.ExportJobStatus;
import com.hoz.hozitech.web.exceptions.ExportException;
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobRepository exportJobRepository;
    private final ExportJobWorker exportJobWorker;
    private final ExportFileStorage exportFileStorage;
    private final ObjectMapper objectMapper;

    @Value("${app.export.ttl-days:7}")
    private int ttlDays;

    @Override
    @Transactional
    public ExportJobResponse createJob(ExportJobRequest request) {
        Map<String, Object> params = request.getParams() == null ? Map.of() : request.getParams();
        ExportJob job = ExportJob.builder()
                .type(request.getType())
                .status(ExportJobStatus.QUEUED)
                .paramsJson(writeParams(params))
                .totalRows(0L)
                .processedRows(0L)
                .progress(0)
                .expiresAt(LocalDateTime.now().plusDays(Math.max(1, ttlDays)))
                .build();

        ExportJob saved = exportJobRepository.save(job);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    exportJobWorker.process(saved.getId());
                }
            });
        } else {
            exportJobWorker.process(saved.getId());
        }
        return ExportJobResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getJob(UUID jobId) {
        return ExportJobResponse.from(findJob(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobDownload getDownload(UUID jobId) {
        ExportJob job = findJob(jobId);
        if (job.getStatus() != ExportJobStatus.SUCCEEDED || job.getFilePath() == null || job.getFilePath().isBlank()) {
            throw new InvalidParamException("Export job is not ready for download");
        }

        Resource resource = exportFileStorage.load(job.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Export file", jobId);
        }

        try {
            return new ExportJobDownload(
                    resource,
                    job.getFileName(),
                    job.getContentType(),
                    resource.contentLength());
        } catch (IOException ex) {
            throw new ExportException("Unable to read export file", ex);
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredJobs() {
        List<ExportJobStatus> statuses = List.of(
                ExportJobStatus.SUCCEEDED,
                ExportJobStatus.FAILED);
        List<ExportJob> expiredJobs = exportJobRepository.findByExpiresAtBeforeAndStatusIn(LocalDateTime.now(), statuses);
        for (ExportJob job : expiredJobs) {
            exportFileStorage.delete(job.getFilePath());
            job.setStatus(ExportJobStatus.EXPIRED);
            job.setFilePath(null);
            job.setFileName(null);
            job.setContentType(null);
            exportJobRepository.save(job);
        }
    }

    private ExportJob findJob(UUID jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job", jobId));
    }

    private String writeParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException ex) {
            throw new InvalidParamException("Export params are invalid");
        }
    }
}
