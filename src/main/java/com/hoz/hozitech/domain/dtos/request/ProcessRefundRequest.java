package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 30, message = "provider must be at most 30 characters")
    private String provider;

    @Size(max = 120, message = "transactionId must be at most 120 characters")
    private String transactionId;

    @Size(max = 10, message = "currency must be at most 10 characters")
    private String currency;

    @Size(max = 2000, message = "rawPayload must be at most 2000 characters")
    private String rawPayload;
}
