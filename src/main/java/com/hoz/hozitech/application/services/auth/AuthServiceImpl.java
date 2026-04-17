package com.hoz.hozitech.application.services.auth;

import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.UserStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.repositories.OtpTokenRepository;
import com.hoz.hozitech.application.repositories.RoleRepository;
import com.hoz.hozitech.application.repositories.TokenRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.repositories.UserSocialAccountRepository;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.security.LoginIdentifierResolver;
import com.hoz.hozitech.security.JwtTokenProvider;
import com.hoz.hozitech.domain.dtos.request.LoginRequest;
import com.hoz.hozitech.domain.dtos.request.RegisterRequest;
import com.hoz.hozitech.domain.dtos.request.SocialLoginRequest;
import com.hoz.hozitech.domain.dtos.response.AuthResponse;
import com.hoz.hozitech.domain.entities.Role;
import com.hoz.hozitech.domain.entities.Token;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.UserSocialAccount;
import com.hoz.hozitech.domain.enums.RoleType;
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;


import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String AUTH_PROVIDER_LOCAL = "LOCAL";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginIdentifierResolver loginIdentifierResolver;

    @Value("${social.google.legacy-fallback-until:}")
    private String googleLegacyFallbackUntilRaw;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationSeconds;

    private LocalDateTime googleLegacyFallbackUntil;

    @PostConstruct
    void initLegacyFallbackWindow() {
        if (googleLegacyFallbackUntilRaw == null || googleLegacyFallbackUntilRaw.isBlank()) {
            googleLegacyFallbackUntil = LocalDateTime.now().plusDays(30);
            log.info("google_legacy_fallback_until={} source=default_plus_30_days", googleLegacyFallbackUntil);
            return;
        }

        String value = googleLegacyFallbackUntilRaw.trim();
        try {
            googleLegacyFallbackUntil = LocalDateTime.parse(value);
            log.info("google_legacy_fallback_until={} source=config_local_datetime", googleLegacyFallbackUntil);
            return;
        } catch (DateTimeParseException ignored) {
            // Try with timezone offset format.
        }

        try {
            googleLegacyFallbackUntil = OffsetDateTime.parse(value).toLocalDateTime();
            log.info("google_legacy_fallback_until={} source=config_offset_datetime", googleLegacyFallbackUntil);
            return;
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException(
                    "Invalid SOCIAL_GOOGLE_LEGACY_FALLBACK_UNTIL format. Use ISO datetime.",
                    ex);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use");
        }
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new ConflictException("Username is already in use");
        }

        Role userRole = roleRepository.findById(RoleType.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", RoleType.USER));

        User user = User.builder()
                .fullName(request.getFullName())
                .userName(request.getUserName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .status(UserStatus.ACTIVE)
                .authProvider("LOCAL")
                .build();

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveUserToken(savedUser, accessToken, "ACCESS", accessTokenExpirationSeconds);
        saveUserToken(savedUser, refreshToken, "REFRESH", refreshTokenExpirationSeconds);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        identifier,
                        request.getPassword()));

        User user = loginIdentifierResolver.resolve(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login credentials"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken, "ACCESS", accessTokenExpirationSeconds);
        saveUserToken(user, refreshToken, "REFRESH", refreshTokenExpirationSeconds);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        if (username != null) {
            User user = userRepository.findByEmailOrUserName(username, username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            CustomUserDetails userDetails = new CustomUserDetails(user);
            Token refreshTokenEntity = tokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

            if (!refreshTokenEntity.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            if (Boolean.TRUE.equals(refreshTokenEntity.getExpired())
                    || Boolean.TRUE.equals(refreshTokenEntity.getRevoked())
                    || refreshTokenEntity.getExpirationDate() == null
                    || refreshTokenEntity.getExpirationDate().isBefore(LocalDateTime.now())) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String accessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken, "ACCESS", accessTokenExpirationSeconds);
                saveUserToken(user, newRefreshToken, "REFRESH", refreshTokenExpirationSeconds);
                return buildAuthResponse(user, accessToken, newRefreshToken);
            }
        }
        throw new UnauthorizedException("Invalid refresh token");
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            // Silently return or decide based on security posture
            throw new InvalidParamException("User with this email not found");
        }

        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        
        // Invalidate all previous unused OTPs for this email
        otpTokenRepository.invalidateAllByEmail(email);
        
        com.hoz.hozitech.domain.entities.OtpToken otpToken = com.hoz.hozitech.domain.entities.OtpToken.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // OTP expires in 5 minutes
                .isUsed(false)
                .build();
                
        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(email, otpCode);
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        com.hoz.hozitech.domain.entities.OtpToken otpToken = otpTokenRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otpCode)
                .orElseThrow(() -> new InvalidParamException("Invalid OTP Code"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidParamException("OTP code has expired");
        }
        
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        com.hoz.hozitech.domain.entities.OtpToken otpToken = otpTokenRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otpCode)
                .orElseThrow(() -> new InvalidParamException("Invalid OTP Code"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidParamException("OTP code has expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
        
        revokeAllUserTokens(user); // Force logout all devices by revoking tokens
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        String provider = normalizeProvider(request.getProvider());
        if (!PROVIDER_GOOGLE.equals(provider)) {
            throw new BusinessException(BusinessErrorCode.UNSUPPORTED_PROVIDER,
                    "Only GOOGLE is supported in this release",
                    HttpStatus.BAD_REQUEST);
        }

        GoogleTokenVerifierService.GoogleTokenPayload googlePayload = googleTokenVerifierService.verify(request.getToken());
        User user = resolveUserForGoogleSocialLogin(googlePayload);
        return issueAuthTokens(user);
    }

    private User resolveUserForGoogleSocialLogin(GoogleTokenVerifierService.GoogleTokenPayload googlePayload) {
        String providerUserId = googlePayload.providerUserId();
        String email = googlePayload.email();

        UserSocialAccount linkedAccount = userSocialAccountRepository
                .findByProviderAndProviderUserId(PROVIDER_GOOGLE, providerUserId)
                .orElse(null);
        if (linkedAccount != null) {
            return linkedAccount.getUser();
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser == null) {
            User newUser = createUserFromGooglePayload(googlePayload);
            ensureGoogleLink(newUser, googlePayload);
            return newUser;
        }

        if (isLocalAccount(existingUser)) {
            throw new BusinessException(BusinessErrorCode.SOCIAL_NOT_LINKED,
                    "Google account is not linked. Please sign in with password and link in Settings.",
                    HttpStatus.CONFLICT);
        }

        if (isLegacyGoogleUser(existingUser) && isLegacyFallbackEnabled()) {
            ensureGoogleLink(existingUser, googlePayload);
            return existingUser;
        }

        throw new BusinessException(BusinessErrorCode.SOCIAL_NOT_LINKED,
                "Google account is not linked. Please sign in with password and link in Settings.",
                HttpStatus.CONFLICT);
    }

    private User createUserFromGooglePayload(GoogleTokenVerifierService.GoogleTokenPayload googlePayload) {
        Role userRole = roleRepository.findById(RoleType.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", RoleType.USER));

        String emailPrefix = googlePayload.email().split("@")[0];
        if (emailPrefix.isBlank()) {
            emailPrefix = "user";
        }

        User user = User.builder()
                .fullName(googlePayload.name() == null || googlePayload.name().isBlank() ? emailPrefix : googlePayload.name())
                .userName(emailPrefix + "_" + System.currentTimeMillis())
                .email(googlePayload.email())
                .avatarUrl(googlePayload.avatarUrl())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(userRole)
                .status(UserStatus.ACTIVE)
                .authProvider(PROVIDER_GOOGLE)
                .build();
        return userRepository.save(user);
    }

    private void ensureGoogleLink(User user, GoogleTokenVerifierService.GoogleTokenPayload googlePayload) {
        String providerUserId = googlePayload.providerUserId();
        String providerEmail = googlePayload.email();

        UserSocialAccount byProviderUserId = userSocialAccountRepository
                .findByProviderAndProviderUserId(PROVIDER_GOOGLE, providerUserId)
                .orElse(null);
        if (byProviderUserId != null && !byProviderUserId.getUser().getId().equals(user.getId())) {
            throw new BusinessException(BusinessErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED,
                    "This Google account is already linked to another user",
                    HttpStatus.CONFLICT);
        }

        UserSocialAccount byUserProvider = userSocialAccountRepository
                .findByUserIdAndProvider(user.getId(), PROVIDER_GOOGLE)
                .orElse(null);
        if (byUserProvider != null) {
            if (!byUserProvider.getProviderUserId().equals(providerUserId)) {
                throw new BusinessException(BusinessErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED,
                        "This account is already linked to another Google identity",
                        HttpStatus.CONFLICT);
            }
            if (providerEmail != null && !providerEmail.isBlank()
                    && !providerEmail.equalsIgnoreCase(byUserProvider.getProviderEmail())) {
                byUserProvider.setProviderEmail(providerEmail);
                byUserProvider.setLinkedAt(LocalDateTime.now());
                userSocialAccountRepository.save(byUserProvider);
            }
            return;
        }

        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(user)
                .provider(PROVIDER_GOOGLE)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .linkedAt(LocalDateTime.now())
                .build();
        userSocialAccountRepository.save(socialAccount);
    }

    private boolean isLegacyFallbackEnabled() {
        return !LocalDateTime.now().isAfter(googleLegacyFallbackUntil);
    }

    private boolean isLocalAccount(User user) {
        return user.getAuthProvider() != null
                && AUTH_PROVIDER_LOCAL.equalsIgnoreCase(user.getAuthProvider());
    }

    private boolean isLegacyGoogleUser(User user) {
        return user.getAuthProvider() != null
                && PROVIDER_GOOGLE.equalsIgnoreCase(user.getAuthProvider());
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private AuthResponse issueAuthTokens(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken, "ACCESS", accessTokenExpirationSeconds);
        saveUserToken(user, refreshToken, "REFRESH", refreshTokenExpirationSeconds);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private void saveUserToken(User user, String jwtToken, String tokenType, long ttlSeconds) {
        LocalDateTime expireDate = LocalDateTime.now().plusSeconds(ttlSeconds);

        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .expirationDate(expireDate)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
        if (validUserTokens.isEmpty())
            return;

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        revokeAllUserTokens(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().getId().name())
                        .build())
                .build();
    }
}
