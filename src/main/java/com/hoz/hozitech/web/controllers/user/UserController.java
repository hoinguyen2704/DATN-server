package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
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
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(responseFactory.success("response.user.profile_fetched",
                userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.user.profile_updated",
                userService.updateProfile(request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(responseFactory.success("response.user.password_changed"));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = fileStorageService.uploadFile(file, "avatars");
        return ResponseEntity.ok(responseFactory.success("response.user.avatar_uploaded",
                userService.uploadAvatar(avatarUrl)));
    }

    @PostMapping("/me/email/change-request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        userService.requestEmailChange(request);
        return ResponseEntity.ok(responseFactory.success("response.user.email_change_otp_sent"));
    }

    @PostMapping("/me/email/verify")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmailChange(@Valid @RequestBody VerifyEmailChangeRequest request) {
        UserResponse response = userService.verifyEmailChange(request);
        return ResponseEntity.ok(responseFactory.success("response.user.email_changed", response));
    }

    @PostMapping("/me/email/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailChangeOtp(@Valid @RequestBody ResendEmailChangeOtpRequest request) {
        userService.resendEmailChangeOtp(request);
        return ResponseEntity.ok(responseFactory.success("response.user.email_change_otp_resent"));
    }

    @GetMapping("/me/social-accounts")
    public ResponseEntity<ApiResponse<List<LinkedSocialAccountResponse>>> getCurrentUserSocialAccounts() {
        return ResponseEntity.ok(responseFactory.success(
                "response.user.social_accounts_fetched",
                userService.getCurrentUserSocialAccounts()));
    }

    @PostMapping("/me/social-accounts/GOOGLE/link-intent")
    public ResponseEntity<ApiResponse<GoogleLinkIntentResponse>> issueGoogleLinkIntent() {
        return ResponseEntity.ok(responseFactory.success(
                "response.user.google_link_intent_issued",
                userService.issueGoogleLinkIntent()));
    }

    @PostMapping("/me/social-accounts/link")
    public ResponseEntity<ApiResponse<LinkedSocialAccountResponse>> linkCurrentUserSocialAccount(
            @Valid @RequestBody LinkSocialAccountRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.user.social_account_linked",
                userService.linkCurrentUserSocialAccount(request)));
    }

    @DeleteMapping("/me/social-accounts/GOOGLE")
    public ResponseEntity<ApiResponse<Void>> unlinkGoogleSocialAccount(
            @Valid @RequestBody UnlinkSocialAccountRequest request) {
        userService.unlinkCurrentUserSocialAccount("GOOGLE", request);
        return ResponseEntity.ok(responseFactory.success("response.user.social_account_unlinked"));
    }
}
