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

    @NotNull(message = "{validation.amount_is_required}")
    @DecimalMin(value = "0.01", message = "{validation.amount_must_be_greater_than_0}")
    private BigDecimal amount;

    @NotBlank(message = "{validation.provider_is_required}")
    @Size(max = 30, message = "{validation.provider_must_be_at_most_30_characters}")
    private String provider;

    @NotBlank(message = "{validation.transactionid_is_required}")
    @Size(max = 120, message = "{validation.transactionid_must_be_at_most_120_characters}")
    private String transactionId;

    @NotBlank(message = "{validation.adminnote_is_required}")
    @Size(max = 1000, message = "{validation.adminnote_must_be_at_most_1000_characters}")
    private String adminNote;

    @Size(max = 10, message = "{validation.currency_must_be_at_most_10_characters}")
    private String currency;

    @Size(max = 2000, message = "{validation.rawpayload_must_be_at_most_2000_characters}")
    private String rawPayload;
}
