package com.hoz.hozitech.application.services.email;

import java.util.Map;

public interface EmailService {
    void sendOtpEmail(String to, String otpCode);

    void sendTemplateMail(String to, String subject, String templateName,
                          Map<String, Object> variables);
}
