package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.LoginRequest;
import com.hoz.hozitech.domain.dtos.request.RegisterRequest;
import com.hoz.hozitech.domain.dtos.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    boolean verifyOtp(String email, String otpCode);

    void resetPassword(String email, String otpCode, String newPassword);

    AuthResponse socialLogin(com.hoz.hozitech.domain.dtos.request.SocialLoginRequest request);
}
