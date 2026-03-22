package com.hoz.hozitech.application.services.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.repositories.OtpTokenRepository;
import com.hoz.hozitech.application.repositories.RoleRepository;
import com.hoz.hozitech.application.repositories.TokenRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.auth.AuthService;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.security.JwtTokenProvider;
import com.hoz.hozitech.domain.dtos.request.LoginRequest;
import com.hoz.hozitech.domain.dtos.request.RegisterRequest;
import com.hoz.hozitech.domain.dtos.request.SocialLoginRequest;
import com.hoz.hozitech.domain.dtos.response.AuthResponse;
import com.hoz.hozitech.domain.entities.Role;
import com.hoz.hozitech.domain.entities.Token;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.RoleType;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${spring.security.oauth2.client.registration.google.client-id:dummy}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        Role userRole = roleRepository.findById(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = User.builder()
                .fullName(request.getFullName())
                .userName(request.getUserName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .status("ACTIVE")
                .authProvider("LOCAL")
                .build();

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveUserToken(savedUser, accessToken);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        if (username != null) {
            User user = userRepository.findByEmailOrUserName(username, username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            CustomUserDetails userDetails = new CustomUserDetails(user);

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String accessToken = jwtService.generateToken(userDetails);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                return buildAuthResponse(user, accessToken, refreshToken);
            }
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            // Silently return or decide based on security posture
            throw new IllegalArgumentException("User with this email not found");
        }

        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Invalidate previous OTPs for this email?
        
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
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP Code"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP code has expired");
        }
        
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        com.hoz.hozitech.domain.entities.OtpToken otpToken = otpTokenRepository.findByEmailAndOtpCodeAndIsUsedFalse(email, otpCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP Code"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP code has expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
        
        revokeAllUserTokens(user); // Force logout all devices by revoking tokens
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        String email;
        String name;
        String avatarUrl = null;

        if ("GOOGLE".equalsIgnoreCase(request.getProvider())) {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(java.util.Collections.singletonList(googleClientId))
                    .build();
            try {
                GoogleIdToken idToken = verifier.verify(request.getToken());
                if (idToken != null) {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                    email = payload.getEmail();
                    name = (String) payload.get("name");
                    avatarUrl = (String) payload.get("picture");
                } else {
                    throw new IllegalArgumentException("Invalid Google token");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Google login failed (" + e.getMessage() + "). Check client-id configuration.");
            }
        } else if ("FACEBOOK".equalsIgnoreCase(request.getProvider())) {
            throw new IllegalArgumentException("Facebook login is not yet configured on backend");
        } else {
            throw new IllegalArgumentException("Unsupported social provider: " + request.getProvider());
        }

        // Check if user exists
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            Role userRole = roleRepository.findById(RoleType.USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

            user = User.builder()
                    .fullName(name)
                    .userName(email.split("@")[0] + "_" + System.currentTimeMillis())
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(userRole)
                    .status("ACTIVE")
                    .authProvider(request.getProvider().toUpperCase())
                    .build();
            user = userRepository.save(user);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private void saveUserToken(User user, String jwtToken) {
        // Find 7 days limit from issue date
        LocalDateTime expireDate = LocalDateTime.now().plusDays(7);

        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType("BEARER")
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
