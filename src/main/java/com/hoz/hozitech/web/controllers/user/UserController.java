package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.EmailChangeRequest;
import com.hoz.hozitech.domain.dtos.request.LinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.ResendEmailChangeOtpRequest;
import com.hoz.hozitech.domain.dtos.request.UnlinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
import com.hoz.hozitech.domain.dtos.request.VerifyEmailChangeRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.GoogleLinkIntentResponse;
import com.hoz.hozitech.domain.dtos.response.LinkedSocialAccountResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestAPI("${api.prefix-client}/users")
@Authenticated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.hoz.hozitech.application.services.storage.FileStorageService fileStorageService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success("Load profile success", userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateProfile(request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = fileStorageService.uploadFile(file, "avatars");
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", userService.uploadAvatar(avatarUrl)));
    }

    @PostMapping("/me/email/change-request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        userService.requestEmailChange(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to new email"));
    }

    @PostMapping("/me/email/verify")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmailChange(@Valid @RequestBody VerifyEmailChangeRequest request) {
        UserResponse response = userService.verifyEmailChange(request);
        return ResponseEntity.ok(ApiResponse.success("Email changed successfully. Please login again", response));
    }

    @PostMapping("/me/email/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailChangeOtp(@Valid @RequestBody ResendEmailChangeOtpRequest request) {
        userService.resendEmailChangeOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully"));
    }

    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<LinkedSocialAccountResponse>>> getCurrentUserSocialAccounts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Load social accounts success",
                userService.getCurrentUserSocialAccounts()));
    }

    @PostMapping("/me/social-accounts/GOOGLE/link-intent")
    public ResponseEntity<ApiResponse<GoogleLinkIntentResponse>> issueGoogleLinkIntent() {
        return ResponseEntity.ok(ApiResponse.success(
                "Issue Google link intent successfully",
                userService.issueGoogleLinkIntent()));
    }

    @PostMapping("/me/social-accounts/link")
    public ResponseEntity<ApiResponse<LinkedSocialAccountResponse>> linkCurrentUserSocialAccount(
            @Valid @RequestBody LinkSocialAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Link social account successfully",
                userService.linkCurrentUserSocialAccount(request)));
    }

    @DeleteMapping("/me/social-accounts/GOOGLE")
    public ResponseEntity<ApiResponse<Void>> unlinkGoogleSocialAccount(
            @Valid @RequestBody UnlinkSocialAccountRequest request) {
        userService.unlinkCurrentUserSocialAccount("GOOGLE", request);
        return ResponseEntity.ok(ApiResponse.success("Unlink social account successfully"));
    }
}
