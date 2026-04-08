package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentWebhookSignatureVerifierTest {

    @Test
    void shouldVerifyValidHexSignature() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                false, 300, "secret-123", "", "", "");
        PaymentWebhookRequest request = sampleRequest();
        long timestamp = Instant.now().getEpochSecond();

        String payload = timestamp + "." + "VNPAY|ORD-001|COMPLETED|evt-001|txn-001|00|1000000|VND";
        String signature = hmacSha256Hex("secret-123", payload);

        assertDoesNotThrow(() -> verifier.verifyOrThrow(request, signature, String.valueOf(timestamp)));
    }

    @Test
    void shouldVerifyValidBase64Signature() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                false, 300, "secret-123", "", "", "");
        PaymentWebhookRequest request = sampleRequest();
        long timestamp = Instant.now().getEpochSecond();

        String payload = timestamp + "." + "VNPAY|ORD-001|COMPLETED|evt-001|txn-001|00|1000000|VND";
        String signature = java.util.Base64.getEncoder().encodeToString(
                hmacSha256("secret-123", payload));

        assertDoesNotThrow(() -> verifier.verifyOrThrow(request, signature, String.valueOf(timestamp)));
    }

    @Test
    void shouldRejectMissingSignatureWhenSecretConfigured() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                false, 300, "secret-123", "", "", "");

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> verifier.verifyOrThrow(sampleRequest(), null, String.valueOf(Instant.now().getEpochSecond())));

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("signature headers"));
    }

    @Test
    void shouldRejectInvalidSignature() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                false, 300, "secret-123", "", "", "");

        assertThrows(
                UnauthorizedException.class,
                () -> verifier.verifyOrThrow(
                        sampleRequest(),
                        "bad-signature",
                        String.valueOf(Instant.now().getEpochSecond())));
    }

    @Test
    void shouldRejectExpiredTimestamp() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                false, 300, "secret-123", "", "", "");
        PaymentWebhookRequest request = sampleRequest();
        long oldTimestamp = Instant.now().minusSeconds(3600).getEpochSecond();

        String payload = oldTimestamp + "." + "VNPAY|ORD-001|COMPLETED|evt-001|txn-001|00|1000000|VND";
        String signature = hmacSha256Hex("secret-123", payload);

        assertThrows(
                UnauthorizedException.class,
                () -> verifier.verifyOrThrow(request, signature, String.valueOf(oldTimestamp)));
    }

    @Test
    void shouldAllowUnsignedWhenNoSecretAndFlagEnabled() {
        PaymentWebhookSignatureVerifier verifier = new PaymentWebhookSignatureVerifier(
                true, 300, "", "", "", "");

        assertDoesNotThrow(() -> verifier.verifyOrThrow(sampleRequest(), null, null));
    }

    private PaymentWebhookRequest sampleRequest() {
        return PaymentWebhookRequest.builder()
                .provider("VNPAY")
                .orderNumber("ORD-001")
                .paymentStatus("COMPLETED")
                .eventId("evt-001")
                .transactionId("txn-001")
                .responseCode("00")
                .amount(new BigDecimal("1000000"))
                .currency("VND")
                .build();
    }

    private static String hmacSha256Hex(String secret, String payload) {
        byte[] bytes = hmacSha256(secret, payload);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build test signature", e);
        }
    }
}
