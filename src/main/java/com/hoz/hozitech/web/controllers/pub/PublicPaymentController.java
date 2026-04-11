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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.hoz.hozitech.application.services.payment.VnpayPaymentService;
import com.hoz.hozitech.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestAPI("${api.prefix-client}/public/payments")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final OrderService orderService;
    private final PaymentWebhookSignatureVerifier webhookSignatureVerifier;
    private final VnpayPaymentService vnpayPaymentService;
    private final ObjectMapper objectMapper;

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

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        try {
            boolean isValid = vnpayPaymentService.verifyIpnSignature(params);
            if (!isValid) {
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
            }

            String orderNumber = params.get("vnp_TxnRef");
            String vnpAmount = params.get("vnp_Amount");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");

            // Look up order through service
            OrderResponse order = null;
            try {
                // Normally we'd need userId, but IPN is system call. 
                // We added a getOrderByNumberForAdmin in OrderService recently, we should use that.
                order = orderService.getOrderByNumberForAdmin(orderNumber);
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
            }

            if (order == null) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
            }

            long amount = Long.parseLong(vnpAmount) / 100;
            if (order.getTotalAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
                return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
            }

            if (PaymentStatus.COMPLETED.name().equals(order.getPaymentStatus())) {
                return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
            }

            String status = "00".equals(responseCode) ? PaymentStatus.COMPLETED.name() : PaymentStatus.FAILED.name();

            String rawPayload = "";
            try {
                rawPayload = objectMapper.writeValueAsString(params);
            } catch (JsonProcessingException ignored) {}

            PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.builder()
                    .provider("VNPAY")
                    .orderNumber(orderNumber)
                    .paymentStatus(status)
                    .eventId(params.getOrDefault("vnp_BankTranNo", UUID.randomUUID().toString()))
                    .transactionId(transactionNo)
                    .responseCode(responseCode)
                    .amount(BigDecimal.valueOf(amount))
                    .currency(params.getOrDefault("vnp_CurrCode", "VND"))
                    .rawPayload(rawPayload)
                    .build();

            // Idempotency key is derived from transactionNo or bankTranNo
            String idempotencyKey = "vnpay_ipn_" + orderNumber + "_" + (transactionNo != null ? transactionNo : UUID.randomUUID().toString());
            orderService.handlePaymentWebhook(webhookRequest, idempotencyKey);

            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }
}
