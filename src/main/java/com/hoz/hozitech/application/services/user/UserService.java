package com.hoz.hozitech.application.services.user;

import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.EmailChangeRequest;
import com.hoz.hozitech.domain.dtos.request.LinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.AdminUpdatePhoneRequest;
import com.hoz.hozitech.domain.dtos.request.ResendEmailChangeOtpRequest;
import com.hoz.hozitech.domain.dtos.request.UnlinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
import com.hoz.hozitech.domain.dtos.request.VerifyEmailChangeRequest;
import com.hoz.hozitech.domain.dtos.response.AuditLogResponse;
import com.hoz.hozitech.domain.dtos.response.GoogleLinkIntentResponse;
import com.hoz.hozitech.domain.dtos.response.LinkedSocialAccountResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import com.hoz.hozitech.domain.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    // User Profile
    UserResponse getCurrentUserProfile();

    UserResponse updateProfile(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    UserResponse uploadAvatar(String avatarUrl); // Typically requires multipart file handling, keeping simple signature
                                                 // for now.

    void requestEmailChange(EmailChangeRequest request);

    UserResponse verifyEmailChange(VerifyEmailChangeRequest request);

    void resendEmailChangeOtp(ResendEmailChangeOtpRequest request);

    // Admin APIs
    PageResponse<UserResponse> getDetailedUsers(String keyword, String role, int page, int size, String sortBy,
            String sortDir);

    UserResponse getUserById(UUID id);

    UserResponse toggleUserStatus(UUID id);

    UserResponse adminUpdatePhone(UUID id, AdminUpdatePhoneRequest request);

    PageResponse<AuditLogResponse> getAuditLogs(String targetType, UUID targetId, int page, int size, String sortBy,
                                                String sortDir);

    List<LinkedSocialAccountResponse> getCurrentUserSocialAccounts();

    GoogleLinkIntentResponse issueGoogleLinkIntent();

    LinkedSocialAccountResponse linkCurrentUserSocialAccount(LinkSocialAccountRequest request);

    LinkedSocialAccountResponse linkGoogleSocialAccountByUserId(UUID userId, String token);

    void unlinkCurrentUserSocialAccount(String provider, UnlinkSocialAccountRequest request);

    // Helper
    User getCurrentUserEntity();
}
