package com.hoz.hozitech.application.services;

public interface EmailService {
    void sendOtpEmail(String to, String otpCode);
}
