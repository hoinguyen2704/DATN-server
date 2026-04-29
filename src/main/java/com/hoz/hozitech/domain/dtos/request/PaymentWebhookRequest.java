package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentWebhookRequest {

    @NotBlank(message = "{validation.provider_is_required}")
    private String provider;

    @NotBlank(message = "{validation.order_number_is_required}")
    private String orderNumber;

    // COMPLETED | FAILED | REFUNDED | PENDING (also supports SUCCESS/PAID aliases)
    @NotBlank(message = "{validation.payment_status_is_required}")
    private String paymentStatus;

    private String eventId;

    private String transactionId;

    private String responseCode;

    @DecimalMin(value = "0.00", inclusive = true, message = "{validation.amount_must_be_greater_than_or_equal_to_0}")
    private BigDecimal amount;

    private String currency;

    private String rawPayload;
}
