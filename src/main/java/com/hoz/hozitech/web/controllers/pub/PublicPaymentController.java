package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.services.order.OrderService;
import com.hoz.hozitech.application.services.order.PaymentWebhookSignatureVerifier;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.web.base.RestAPI;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestAPI("${api.prefix-client}/public/payments")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final OrderService orderService;
    private final PaymentWebhookSignatureVerifier webhookSignatureVerifier;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<OrderResponse>> handleWebhook(
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String webhookSignature,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String webhookTimestamp,
            @Valid @RequestBody PaymentWebhookRequest request) {
        webhookSignatureVerifier.verifyOrThrow(request, webhookSignature, webhookTimestamp);
        OrderResponse response = orderService.handlePaymentWebhook(request, webhookId);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", response));
    }
}
