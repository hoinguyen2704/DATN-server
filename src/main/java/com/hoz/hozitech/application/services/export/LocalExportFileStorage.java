package com.hoz.hozitech.application.services.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
public class LocalExportFileStorage implements ExportFileStorage {

    private final Path root;

    public LocalExportFileStorage(
            @Value("${app.export.storage-dir:storage/exports}") String storageDir) throws IOException {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public Path createJobDirectory(UUID jobId) throws IOException {
        Path directory = root.resolve(jobId.toString()).normalize();
        if (!directory.startsWith(root)) {
            throw new IOException("Invalid export job directory");
        }
        Files.createDirectories(directory);
        return directory;
    }

    @Override
    public Resource load(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            return new FileSystemResource(root.resolve("__invalid_export_file__"));
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            log.warn("Skip deleting export file outside storage root: {}", filePath);
            return;
        }
        Path target = Files.isDirectory(path) ? path : path.getParent();
        if (target == null || !target.startsWith(root) || !Files.exists(target)) {
            return;
        }
        try (var stream = Files.walk(target)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(item -> {
                        try {
                            Files.deleteIfExists(item);
                        } catch (IOException ex) {
                            log.warn("Failed to delete export path {}: {}", item, ex.getMessage());
                        }
                    });
        } catch (IOException ex) {
            log.warn("Failed to cleanup export path {}: {}", target, ex.getMessage());
        }
    }
}
