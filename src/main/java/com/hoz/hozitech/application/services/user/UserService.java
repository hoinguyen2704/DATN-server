package com.hoz.hozitech.application.services.user;

import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.LinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UnlinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
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

    // Admin APIs
    PageResponse<UserResponse> getDetailedUsers(String keyword, String role, int page, int size, String sortBy,
            String sortDir);

    UserResponse getUserById(UUID id);

    UserResponse toggleUserStatus(UUID id);

    List<LinkedSocialAccountResponse> getCurrentUserSocialAccounts();

    LinkedSocialAccountResponse linkCurrentUserSocialAccount(LinkSocialAccountRequest request);

    void unlinkCurrentUserSocialAccount(String provider, UnlinkSocialAccountRequest request);

    // Helper
    User getCurrentUserEntity();
}
