package com.hoz.hozitech.application.config.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "momo")
public class MomoProperties {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String createPath = "/create";
    private String redirectUrl;
    private String ipnUrl;
    private String requestType = "captureWallet";
    private boolean autoCapture = true;
    private int pendingTimeoutMinutes = 30;
}
