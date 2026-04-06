package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentWebhookRequest {

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    // COMPLETED | FAILED | REFUNDED | PENDING (also supports SUCCESS/PAID aliases)
    @NotBlank(message = "Payment status is required")
    private String paymentStatus;

    private String eventId;

    private String transactionId;

    private String responseCode;

    private String rawPayload;
}
