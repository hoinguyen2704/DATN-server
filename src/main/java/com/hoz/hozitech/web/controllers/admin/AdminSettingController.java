package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.domain.dtos.request.SettingRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.SettingResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestAPI("${api.prefix-admin}/settings")
@RoleAdmin
@RequiredArgsConstructor
public class AdminSettingController {

    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<SettingResponse>>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.success("Fetch settings successfully",
                settingService.getAllSettings()));
    }

    @GetMapping("/{group}")
    public ResponseEntity<ApiResponse<List<SettingResponse>>> getByGroup(@PathVariable String group) {
        return ResponseEntity.ok(ApiResponse.success("Fetch settings successfully",
                settingService.getSettingsByGroup(group.toUpperCase())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> batchUpdate(@Valid @RequestBody List<SettingRequest> requests) {
        settingService.batchUpdate(requests);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully"));
    }
}
