package com.hoz.hozitech.application.services.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.repositories.ExportJobRepository;
import com.hoz.hozitech.domain.entities.ExportJob;
import com.hoz.hozitech.domain.enums.ExportJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportJobWorker {

    private static final int ERROR_MESSAGE_LIMIT = 4000;

    private final ExportJobRepository exportJobRepository;
    private final ExportFileStorage exportFileStorage;
    private final ChunkedExcelExportGenerator exportGenerator;
    private final ObjectMapper objectMapper;

    @Async("exportExecutor")
    public void process(UUID jobId) {
        try {
            markRunning(jobId);
            ExportJob job = findJob(jobId);
            Map<String, Object> params = readParams(job.getParamsJson());
            Path jobDirectory = exportFileStorage.createJobDirectory(jobId);

            ChunkedExcelExportGenerator.GeneratedExport generated = exportGenerator.generate(
                    job.getType(),
                    params,
                    jobDirectory,
                    (processedRows, totalRows) -> updateProgress(jobId, processedRows, totalRows));

            markSucceeded(jobId, generated);
        } catch (Exception ex) {
            log.error("Export job {} failed", jobId, ex);
            markFailed(jobId, ex);
        }
    }

    private void markRunning(UUID jobId) {
        ExportJob job = findJob(jobId);
        job.setStatus(ExportJobStatus.RUNNING);
        job.setProcessedRows(0L);
        job.setTotalRows(0L);
        job.setProgress(0);
        job.setErrorMessage(null);
        exportJobRepository.save(job);
    }

    private void updateProgress(UUID jobId, long processedRows, long totalRows) {
        ExportJob job = findJob(jobId);
        if (job.getStatus() != ExportJobStatus.RUNNING) {
            return;
        }
        int progress = totalRows <= 0 ? 100 : (int) Math.min(99, Math.floor((processedRows * 100.0) / totalRows));
        job.setTotalRows(totalRows);
        job.setProcessedRows(processedRows);
        job.setProgress(progress);
        exportJobRepository.save(job);
    }

    private void markSucceeded(UUID jobId, ChunkedExcelExportGenerator.GeneratedExport generated) {
        ExportJob job = findJob(jobId);
        job.setStatus(ExportJobStatus.SUCCEEDED);
        job.setTotalRows(generated.totalRows());
        job.setProcessedRows(generated.totalRows());
        job.setProgress(100);
        job.setFilePath(generated.path().toString());
        job.setFileName(generated.fileName());
        job.setContentType(generated.contentType());
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);
    }

    private void markFailed(UUID jobId, Exception ex) {
        exportJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        });
    }

    private ExportJob findJob(UUID jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Export job not found: " + jobId));
    }

    private Map<String, Object> readParams(String paramsJson) throws Exception {
        if (paramsJson == null || paramsJson.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(paramsJson, new TypeReference<>() {
        });
    }

    private String truncate(String message) {
        if (message.length() <= ERROR_MESSAGE_LIMIT) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_LIMIT);
    }
}
