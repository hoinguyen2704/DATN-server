package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.services.order.OrderService;
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

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<OrderResponse>> handleWebhook(
            @RequestHeader(value = "X-Webhook-Id", required = false) String webhookId,
            @Valid @RequestBody PaymentWebhookRequest request) {
        OrderResponse response = orderService.handlePaymentWebhook(request, webhookId);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", response));
    }
}
