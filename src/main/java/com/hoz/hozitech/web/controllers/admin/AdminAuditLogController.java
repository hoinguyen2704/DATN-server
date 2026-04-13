package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.AuditLogResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/audit-logs")
@RoleAdmin
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_SIZE_LARGE_STR) int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir) {
        PageResponse<AuditLogResponse> logs = userService.getAuditLogs(targetType, targetId, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Fetch audit logs successfully", logs));
    }
}
