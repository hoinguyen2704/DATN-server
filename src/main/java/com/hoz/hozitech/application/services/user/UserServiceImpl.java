package com.hoz.hozitech.application.services.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

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
import com.hoz.hozitech.application.repositories.EmailChangeOtpRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.repositories.UserSocialAccountRepository;
import com.hoz.hozitech.application.services.auth.GoogleTokenVerifierService;
import com.hoz.hozitech.application.services.audit.AuditLogService;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.config.utils.PhoneNumberUtils;
import com.hoz.hozitech.application.specifications.UserSpecification;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.AdminUpdatePhoneRequest;
import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.EmailChangeRequest;
import com.hoz.hozitech.domain.dtos.request.LinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.ResendEmailChangeOtpRequest;
import com.hoz.hozitech.domain.dtos.request.UnlinkSocialAccountRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
import com.hoz.hozitech.domain.dtos.request.VerifyEmailChangeRequest;
import com.hoz.hozitech.domain.dtos.response.AuditLogResponse;
import com.hoz.hozitech.domain.dtos.response.LinkedSocialAccountResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import com.hoz.hozitech.domain.entities.EmailChangeOtp;
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
    private static final int EMAIL_CHANGE_OTP_TTL_MINUTES = 5;
    private static final int EMAIL_CHANGE_OTP_RATE_WINDOW_MINUTES = 15;
    private static final int EMAIL_CHANGE_OTP_MAX_REQUESTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailChangeOtpRepository emailChangeOtpRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
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
        String currentPhoneNumber = normalizeOptionalPhoneNumber(user.getPhoneNumber());

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getDateOfBirth() != null)
            user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            user.setGender(request.getGender());
        if (request.getPhoneNumber() != null) {
            String requestedPhoneNumber = normalizeOptionalPhoneNumber(request.getPhoneNumber());

            if (currentPhoneNumber != null) {
                if (requestedPhoneNumber == null || !currentPhoneNumber.equals(requestedPhoneNumber)) {
                    throw new InvalidParamException("Phone number is already set and cannot be changed here");
                }
            } else if (requestedPhoneNumber != null) {
                ensurePhoneNumberAvailable(requestedPhoneNumber, user.getId());
                user.setPhoneNumber(requestedPhoneNumber);
            }
        }

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
    @Transactional
    public void requestEmailChange(EmailChangeRequest request) {
        User user = getCurrentUserEntity();
        String newEmail = normalizeEmail(request.getNewEmail());

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new InvalidParamException("New email must be different from current email");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidParamException("Current password is incorrect");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email is already in use");
        }

        enforceEmailChangeRateLimit(user.getId());
        issueEmailChangeOtp(user, newEmail);
    }

    @Override
    @Transactional
    public UserResponse verifyEmailChange(VerifyEmailChangeRequest request) {
        User user = getCurrentUserEntity();
        String newEmail = normalizeEmail(request.getNewEmail());

        EmailChangeOtp otp = emailChangeOtpRepository.findValidOtp(user.getId(), newEmail, request.getOtpCode())
                .orElseThrow(() -> new InvalidParamException("Invalid OTP code"));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidParamException("OTP code has expired");
        }

        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email is already in use");
        }

        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        User savedUser = userRepository.save(user);

        otp.setIsUsed(true);
        emailChangeOtpRepository.save(otp);
        emailChangeOtpRepository.invalidateAllByUserId(user.getId());

        auditLogService.record(
                savedUser,
                "USER_CHANGE_EMAIL",
                "USER",
                savedUser.getId(),
                oldEmail,
                newEmail,
                "SELF_SERVICE");

        return mapToResponse(savedUser);
    }

    @Override
    @Transactional
    public void resendEmailChangeOtp(ResendEmailChangeOtpRequest request) {
        User user = getCurrentUserEntity();
        String newEmail = normalizeEmail(request.getNewEmail());

        List<EmailChangeOtp> pendingRequests = emailChangeOtpRepository.findPendingRequests(user.getId(), newEmail);
        if (pendingRequests.isEmpty()) {
            throw new InvalidParamException("No pending email change request for this email");
        }
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email is already in use");
        }

        enforceEmailChangeRateLimit(user.getId());
        issueEmailChangeOtp(user, newEmail);
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
        User actor = getCurrentUserEntity();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole().getId() == RoleType.ADMIN) {
            throw new InvalidParamException("Cannot lock an admin account");
        }

        UserStatus previousStatus = user.getStatus();
        if (UserStatus.ACTIVE == user.getStatus()) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        User savedUser = userRepository.save(user);

        auditLogService.record(
                actor,
                "ADMIN_TOGGLE_USER_STATUS",
                "USER",
                savedUser.getId(),
                previousStatus.name(),
                savedUser.getStatus().name(),
                null);

        return mapToResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse adminUpdatePhone(UUID id, AdminUpdatePhoneRequest request) {
        User actor = getCurrentUserEntity();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedPhone = normalizeRequiredPhoneNumber(request.getPhoneNumber());
        ensurePhoneNumberAvailable(normalizedPhone, target.getId());

        String oldPhone = target.getPhoneNumber();
        if (oldPhone != null && oldPhone.equals(normalizedPhone)) {
            throw new InvalidParamException("Phone number is unchanged");
        }

        target.setPhoneNumber(normalizedPhone);
        User savedUser = userRepository.save(target);

        auditLogService.record(
                actor,
                "ADMIN_UPDATE_USER_PHONE",
                "USER",
                savedUser.getId(),
                oldPhone,
                normalizedPhone,
                request.getReason().trim());

        return mapToResponse(savedUser);
    }

    @Override
    public PageResponse<AuditLogResponse> getAuditLogs(String targetType, UUID targetId, int page, int size,
                                                       String sortBy, String sortDir) {
        return auditLogService.getAuditLogs(targetType, targetId, page, size, sortBy, sortDir);
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

    private void issueEmailChangeOtp(User user, String newEmail) {
        String otpCode = generateOtpCode();
        emailChangeOtpRepository.invalidateAllByUserId(user.getId());
        emailChangeOtpRepository.save(EmailChangeOtp.builder()
                .user(user)
                .newEmail(newEmail)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(EMAIL_CHANGE_OTP_TTL_MINUTES))
                .isUsed(false)
                .build());

        emailService.sendTemplateMail(
                newEmail,
                "Mã xác thực đổi email - HoziTech",
                "otp-email",
                Map.of("otpCode", otpCode));
    }

    private void enforceEmailChangeRateLimit(UUID userId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(EMAIL_CHANGE_OTP_RATE_WINDOW_MINUTES);
        long recentRequests = emailChangeOtpRepository.countRecentRequests(userId, since);
        if (recentRequests >= EMAIL_CHANGE_OTP_MAX_REQUESTS) {
            throw new InvalidParamException("Too many OTP requests. Please try again later");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidParamException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredPhoneNumber(String phoneNumber) {
        return PhoneNumberUtils.normalizeVietnamesePhoneNumber(phoneNumber)
                .orElseThrow(() -> new InvalidParamException("Invalid Vietnamese phone number format"));
    }

    private String normalizeOptionalPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        return normalizeRequiredPhoneNumber(phoneNumber);
    }

    private void ensurePhoneNumberAvailable(String normalizedPhoneNumber, UUID excludedUserId) {
        if (normalizedPhoneNumber == null) {
            return;
        }

        for (String candidate : PhoneNumberUtils.buildLookupCandidates(normalizedPhoneNumber)) {
            userRepository.findByPhoneNumber(candidate)
                    .filter(existing -> !existing.getId().equals(excludedUserId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Phone number is already in use");
                    });
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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
