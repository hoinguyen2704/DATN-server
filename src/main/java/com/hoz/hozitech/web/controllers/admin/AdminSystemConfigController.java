package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.SystemConfigService;
import com.hoz.hozitech.domain.dtos.request.SystemConfigRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.SystemConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/system-configs")
@RoleAdmin
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SystemConfigResponse>>> getAllConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch system configs successfully", systemConfigService.getAllConfigs(page, size)));
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getConfigByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success("Fetch config successfully", systemConfigService.getConfigByKey(key)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemConfigResponse>> saveOrUpdateConfig(@Valid @RequestBody SystemConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Config saved successfully", systemConfigService.saveOrUpdateConfig(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable UUID id) {
        systemConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success("Config deleted successfully"));
    }
}
