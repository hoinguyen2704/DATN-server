package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.domain.dtos.request.ExportJobRequest;
import com.hoz.hozitech.domain.dtos.response.ExportJobResponse;

import java.util.UUID;

public interface ExportJobService {

    ExportJobResponse createJob(ExportJobRequest request);

    ExportJobResponse getJob(UUID jobId);

    ExportJobDownload getDownload(UUID jobId);

    void cleanupExpiredJobs();
}
