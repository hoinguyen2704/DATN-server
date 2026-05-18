package com.hoz.hozitech.application.services.auth;

import com.hoz.hozitech.domain.dtos.request.LoginRequest;
import com.hoz.hozitech.domain.dtos.request.RegisterRequest;
import com.hoz.hozitech.domain.dtos.request.SocialLoginRequest;
import com.hoz.hozitech.domain.dtos.response.AuthResponse;
import com.hoz.hozitech.domain.dtos.response.GoogleLoginExchangeResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    boolean verifyOtp(String email, String otpCode);

    void resetPassword(String email, String otpCode, String newPassword);

    AuthResponse socialLogin(SocialLoginRequest request);

    String buildGoogleAuthorizationUrl(String state);

    String exchangeGoogleAuthorizationCodeForIdToken(String code);

    String createGoogleLoginTicketFromAuthorizationCode(String code, String redirectTo);

    GoogleLoginExchangeResponse exchangeGoogleLoginTicket(String ticket);

    void logout(java.util.UUID userId);
}
