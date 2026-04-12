package com.hoz.hozitech.application.services.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.repositories.UserSocialAccountRepository;
import com.hoz.hozitech.application.services.auth.GoogleTokenVerifierService;
import com.hoz.hozitech.application.specifications.UserSpecification;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.LinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UnlinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
import com.hoz.hozitech.domain.dtos.response.LinkedSocialAccountResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.UserSocialAccount;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.RoleType;
import com.hoz.hozitech.domain.enums.UserStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String AUTH_PROVIDER_LOCAL = "LOCAL";

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String identifier = authentication.getName(); // the loaded username logic defaults to email
        return userRepository.findByEmailOrUserName(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {
        User user = getCurrentUserEntity();

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getDateOfBirth() != null)
            user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            user.setGender(request.getGender());

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidParamException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(String avatarUrl) {
        User user = getCurrentUserEntity();
        user.setAvatarUrl(avatarUrl);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserResponse> getDetailedUsers(String keyword, String role, int page, int size, String sortBy,
            String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // page - 1 because Spring Data JPA is 0-indexed
        Pageable pageable = PaginationConstant.of(page, size, sort);

        Specification<User> spec = Specification.where(UserSpecification.hasFullNameOrEmail(keyword));

        if (role != null && !role.isBlank()) {
            try {
                RoleType roleType = RoleType.valueOf(role.toUpperCase());
                spec = spec.and(UserSpecification.hasRoleType(roleType));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<User> users = userRepository.findAll(spec, pageable);
        Page<UserResponse> responsePage = users.map(this::mapToResponse);

        return PageResponse.of(responsePage);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole().getId() == RoleType.ADMIN) {
            throw new InvalidParamException("Cannot lock an admin account");
        }

        if (UserStatus.ACTIVE == user.getStatus()) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public List<LinkedSocialAccountResponse> getCurrentUserSocialAccounts() {
        User user = getCurrentUserEntity();
        Optional<UserSocialAccount> google = userSocialAccountRepository
                .findByUserIdAndProvider(user.getId(), PROVIDER_GOOGLE);

        LinkedSocialAccountResponse googleResponse = google
                .map(this::mapToLinkedSocialResponse)
                .orElseGet(() -> LinkedSocialAccountResponse.builder()
                        .provider(PROVIDER_GOOGLE)
                        .linked(false)
                        .build());

        return List.of(googleResponse);
    }

    @Override
    @Transactional
    public LinkedSocialAccountResponse linkCurrentUserSocialAccount(LinkSocialAccountRequest request) {
        String provider = normalizeProvider(request.getProvider());
        if (!PROVIDER_GOOGLE.equals(provider)) {
            throw new BusinessException(BusinessErrorCode.UNSUPPORTED_PROVIDER,
                    "Only GOOGLE is supported in this release",
                    HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUserEntity();
        GoogleTokenVerifierService.GoogleTokenPayload googlePayload = googleTokenVerifierService.verify(request.getToken());

        if (!user.getEmail().equalsIgnoreCase(googlePayload.email())) {
            throw new BusinessException(BusinessErrorCode.GOOGLE_EMAIL_MISMATCH,
                    "Google account email must match your current account email",
                    HttpStatus.CONFLICT);
        }

        Optional<UserSocialAccount> existingByProviderUserId = userSocialAccountRepository
                .findByProviderAndProviderUserId(provider, googlePayload.providerUserId());
        if (existingByProviderUserId.isPresent()) {
            UserSocialAccount existing = existingByProviderUserId.get();
            if (existing.getUser().getId().equals(user.getId())) {
                return mapToLinkedSocialResponse(existing);
            }
            throw new BusinessException(BusinessErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED,
                    "This Google account is already linked to another user",
                    HttpStatus.CONFLICT);
        }

        Optional<UserSocialAccount> existingByUserProvider = userSocialAccountRepository
                .findByUserIdAndProvider(user.getId(), provider);
        if (existingByUserProvider.isPresent()) {
            throw new BusinessException(BusinessErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED,
                    "Your account is already linked to another Google identity",
                    HttpStatus.CONFLICT);
        }

        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(googlePayload.providerUserId())
                .providerEmail(googlePayload.email())
                .linkedAt(LocalDateTime.now())
                .build();

        return mapToLinkedSocialResponse(userSocialAccountRepository.save(socialAccount));
    }

    @Override
    @Transactional
    public void unlinkCurrentUserSocialAccount(String provider, UnlinkSocialAccountRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        if (!PROVIDER_GOOGLE.equals(normalizedProvider)) {
            throw new BusinessException(BusinessErrorCode.UNSUPPORTED_PROVIDER,
                    "Only GOOGLE is supported in this release",
                    HttpStatus.BAD_REQUEST);
        }

        User user = getCurrentUserEntity();
        UserSocialAccount socialAccount = userSocialAccountRepository
                .findByUserIdAndProvider(user.getId(), normalizedProvider)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.SOCIAL_ACCOUNT_NOT_LINKED,
                        "Google account is not linked",
                        HttpStatus.BAD_REQUEST));

        long linkedProviderCount = userSocialAccountRepository.countByUserId(user.getId());
        long remainingLinkedProviders = Math.max(0L, linkedProviderCount - 1L);
        long localMethod = isLocalAccount(user) ? 1L : 0L;
        long remainingMethods = localMethod + remainingLinkedProviders;
        if (remainingMethods <= 0L) {
            throw new BusinessException(BusinessErrorCode.UNLINK_LAST_LOGIN_METHOD,
                    "Cannot unlink the last login method",
                    HttpStatus.CONFLICT);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidParamException("Current password is incorrect");
        }

        userSocialAccountRepository.delete(socialAccount);
    }

    private boolean isLocalAccount(User user) {
        return user.getAuthProvider() != null && AUTH_PROVIDER_LOCAL.equalsIgnoreCase(user.getAuthProvider());
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private LinkedSocialAccountResponse mapToLinkedSocialResponse(UserSocialAccount socialAccount) {
        return LinkedSocialAccountResponse.builder()
                .provider(socialAccount.getProvider())
                .linked(true)
                .email(socialAccount.getProviderEmail())
                .linkedAt(socialAccount.getLinkedAt())
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole().getId().name()) // Assuming Role.getId() returns RoleType enum
                .createdAt(user.getCreatedAt())
                .build();
    }
}
