package com.hoz.hozitech.application.config.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bank-transfer")
public class BankTransferProperties {
    private int pendingTimeoutMinutes = 1440; // 1 day
}
