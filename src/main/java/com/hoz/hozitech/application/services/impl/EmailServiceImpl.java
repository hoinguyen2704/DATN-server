package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

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

    @Override
    @Async
    public void sendTemplateMail(String to, String subject, String templateName,
                                  Map<String, Object> variables) {
        log.info("Preparing template email [{}] to {}", templateName, to);

        try {
            Context context = new Context();
            context.setVariables(variables);

            String content = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }

            mailSender.send(mimeMessage);
            log.info("Template email [{}] sent successfully to {}", templateName, to);

        } catch (MessagingException e) {
            log.error("Failed to send template email [{}] to {}", templateName, to, e);
        }
    }
}
