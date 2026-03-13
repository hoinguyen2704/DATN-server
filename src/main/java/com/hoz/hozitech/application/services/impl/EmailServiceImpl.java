package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String to, String otpCode) {
        log.info("Preparing OTP email to {} with code: {}", to, otpCode);
        
        try {
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            
            // Render template
            String process = templateEngine.process("otp-email", context);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setTo(to);
            helper.setSubject("Mã xác thực Đặt Lại Mật Khẩu - HoziTech");
            helper.setText(process, true);
            
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            
            mailSender.send(mimeMessage);
            log.info("OTP Email sent successfully to {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", to, e);
            throw new RuntimeException("Failed to send OTP email");
        }
    }
}
