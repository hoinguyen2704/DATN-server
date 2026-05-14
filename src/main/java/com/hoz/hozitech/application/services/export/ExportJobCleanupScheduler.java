package com.hoz.hozitech.application.services.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportJobCleanupScheduler {

    private final ExportJobService exportJobService;

    @Scheduled(fixedDelayString = "${app.export.cleanup-interval-ms:3600000}")
    public void cleanupExpiredJobs() {
        try {
            exportJobService.cleanupExpiredJobs();
        } catch (Exception ex) {
            log.warn("Failed to cleanup expired export jobs: {}", ex.getMessage());
        }
    }
}
