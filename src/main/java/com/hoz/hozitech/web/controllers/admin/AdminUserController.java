package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.storage.FileStorageService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.export.ExportService;
import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.domain.dtos.request.AdminCreateUserRequest;
import com.hoz.hozitech.domain.dtos.request.AdminUpdatePhoneRequest;
import com.hoz.hozitech.domain.dtos.request.AdminUpdateUserProfileRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import com.hoz.hozitech.domain.enums.UserStatus;

@RestAPI("${api.prefix-admin}/users")
@RoleAdmin
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final ExportService exportService;
    private final FileStorageService fileStorageService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_SIZE_LARGE_STR) int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir
    ) {
        PageResponse<UserResponse> users = userService.getDetailedUsers(keyword, role, page, size, sortBy, sortDir);
        return ResponseEntity.ok(responseFactory.success("response.admin_user.list_fetched", users));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createCustomer(
            @Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.admin_user.customer_created",
                userService.adminCreateCustomer(request)));
    }

    @PostMapping("/avatar-upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = fileStorageService.uploadFile(file, "avatars");
        return ResponseEntity.ok(responseFactory.success("response.admin_user.avatar_uploaded",
                Map.of("avatarUrl", avatarUrl)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(responseFactory.success("response.admin_user.fetched", userService.getUserById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable UUID id) {
        UserResponse response = userService.toggleUserStatus(id);
        String messageKey = UserStatus.LOCKED == response.getStatus()
                ? "response.admin_user.locked"
                : "response.admin_user.unlocked";
        return ResponseEntity.ok(responseFactory.success(messageKey, response));
    }

    @PatchMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserProfileRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.admin_user.profile_updated",
                userService.adminUpdateProfile(id, request)));
    }

    @PatchMapping("/{id}/phone")
    public ResponseEntity<ApiResponse<UserResponse>> updatePhone(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdatePhoneRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.admin_user.phone_updated",
                userService.adminUpdatePhone(id, request)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        byte[] data = exportService.exportUsersToExcel(keyword, role);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
