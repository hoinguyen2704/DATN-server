package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.services.export.ExportJobDownload;
import com.hoz.hozitech.application.services.export.ExportJobService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.request.ExportJobRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.ExportJobResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/export-jobs")
@RoleAdmin
@RequiredArgsConstructor
public class AdminExportJobController {

    private final ExportJobService exportJobService;
    private final LocalizedApiResponseFactory responseFactory;

    @PostMapping
    public ResponseEntity<ApiResponse<ExportJobResponse>> createJob(@Valid @RequestBody ExportJobRequest request) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(responseFactory.success("response.admin_export_job.created", exportJobService.createJob(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExportJobResponse>> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(responseFactory.success("response.admin_export_job.fetched", exportJobService.getJob(id)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        ExportJobDownload download = exportJobService.getDownload(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(download.fileName()).build());
        headers.setContentLength(download.contentLength());
        return new ResponseEntity<>(download.resource(), headers, HttpStatus.OK);
    }
}
