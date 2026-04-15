package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRefundRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "provider is required")
    @Size(max = 30, message = "provider must be at most 30 characters")
    private String provider;

    @NotBlank(message = "transactionId is required")
    @Size(max = 120, message = "transactionId must be at most 120 characters")
    private String transactionId;

    @NotBlank(message = "adminNote is required")
    @Size(max = 1000, message = "adminNote must be at most 1000 characters")
    private String adminNote;

    @Size(max = 10, message = "currency must be at most 10 characters")
    private String currency;

    @Size(max = 2000, message = "rawPayload must be at most 2000 characters")
    private String rawPayload;
}
