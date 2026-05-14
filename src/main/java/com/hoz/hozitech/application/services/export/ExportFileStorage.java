package com.hoz.hozitech.application.services.export;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface ExportFileStorage {
    Path createJobDirectory(UUID jobId) throws IOException;

    Resource load(String filePath);

    void delete(String filePath);
}
