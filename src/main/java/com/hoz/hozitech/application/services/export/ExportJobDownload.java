package com.hoz.hozitech.application.services.export;

import org.springframework.core.io.Resource;

public record ExportJobDownload(
        Resource resource,
        String fileName,
        String contentType,
        long contentLength) {
}
