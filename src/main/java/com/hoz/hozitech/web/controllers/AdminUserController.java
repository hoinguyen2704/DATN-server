package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.UserService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-admin}/users")
@RequiredArgsConstructor
// Use this if security relies on method level, otherwise it's handled in SecurityConfig
// @PreAuthorize("hasAuthority('ADMIN')") 
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir
    ) {
        PageResponse<UserResponse> users = userService.getDetailedUsers(keyword, role, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Fetch users successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch user detail success", userService.getUserById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable UUID id) {
        UserResponse response = userService.toggleUserStatus(id);
        String msg = "LOCKED".equalsIgnoreCase(response.getStatus()) ? "User has been locked successfully" : "User has been unlocked successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }
}
