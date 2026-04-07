package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.PaymentWebhookRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@Component
public class PaymentWebhookSignatureVerifier {

    private static final long MAX_UNIX_SECOND = 9_999_999_999L;
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final boolean allowUnsigned;
    private final long maxTimestampSkewSeconds;
    private final String defaultSecret;
    private final String vnpaySecret;
    private final String momoSecret;
    private final String bankTransferSecret;

    public PaymentWebhookSignatureVerifier(
            @Value("${payment.webhook.allow-unsigned:false}") boolean allowUnsigned,
            @Value("${payment.webhook.max-timestamp-skew-seconds:300}") long maxTimestampSkewSeconds,
            @Value("${payment.webhook.default-secret:}") String defaultSecret,
            @Value("${payment.webhook.vnpay-secret:}") String vnpaySecret,
            @Value("${payment.webhook.momo-secret:}") String momoSecret,
            @Value("${payment.webhook.bank-transfer-secret:}") String bankTransferSecret) {
        this.allowUnsigned = allowUnsigned;
        this.maxTimestampSkewSeconds = maxTimestampSkewSeconds;
        this.defaultSecret = defaultSecret;
        this.vnpaySecret = vnpaySecret;
        this.momoSecret = momoSecret;
        this.bankTransferSecret = bankTransferSecret;
    }

    public void verifyOrThrow(PaymentWebhookRequest request, String signatureHeader, String timestampHeader) {
        String provider = normalizeProvider(request.getProvider());
        String secret = resolveSecret(provider);
        boolean hasSecret = hasText(secret);
        boolean hasSignature = hasText(signatureHeader);
        boolean hasTimestamp = hasText(timestampHeader);

        if (!hasSecret) {
            if (allowUnsigned) {
                log.warn("Skipping webhook signature verification: no secret configured for provider={}", provider);
                return;
            }
            throw new UnauthorizedException("Webhook secret is not configured for provider: " + provider);
        }

        if (!hasSignature || !hasTimestamp) {
            throw new UnauthorizedException("Webhook signature headers are missing");
        }

        long timestamp = parseTimestampSeconds(timestampHeader);
        long nowSeconds = Instant.now().getEpochSecond();
        if (Math.abs(nowSeconds - timestamp) > maxTimestampSkewSeconds) {
            throw new UnauthorizedException("Webhook timestamp is outside allowed window");
        }

        String providedSignature = normalizeSignature(signatureHeader);
        if (!hasText(providedSignature)) {
            throw new UnauthorizedException("Webhook signature is empty");
        }

        String payload = buildPayload(request);
        String signedPayload = timestamp + "." + payload;
        byte[] hmacBytes = computeHmac(secret, signedPayload);
        String expectedHex = toHex(hmacBytes);
        String expectedBase64 = Base64.getEncoder().encodeToString(hmacBytes);
        String expectedBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

        if (!safeEquals(providedSignature, expectedHex)
                && !safeEquals(providedSignature, expectedBase64)
                && !safeEquals(providedSignature, expectedBase64Url)) {
            throw new UnauthorizedException("Invalid webhook signature");
        }
    }

    private String resolveSecret(String provider) {
        return switch (provider) {
            case "VNPAY" -> firstNonBlank(vnpaySecret, defaultSecret);
            case "MOMO" -> firstNonBlank(momoSecret, defaultSecret);
            case "BANK_TRANSFER" -> firstNonBlank(bankTransferSecret, defaultSecret);
            default -> firstNonBlank(defaultSecret);
        };
    }

    private String normalizeProvider(String provider) {
        return hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
    }

    private long parseTimestampSeconds(String value) {
        try {
            long raw = Long.parseLong(value.trim());
            return raw > MAX_UNIX_SECOND ? raw / 1000 : raw;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid webhook timestamp");
        }
    }

    private String normalizeSignature(String value) {
        if (!hasText(value)) return null;
        String trimmed = value.trim();

        // Stripe-like format: "t=...,v1=..."
        if (trimmed.contains(",")) {
            for (String part : trimmed.split(",")) {
                String p = part.trim();
                if (p.startsWith("v1=")) {
                    return p.substring(3).trim();
                }
            }
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("sha256=")) return trimmed.substring(7).trim();
        if (lower.startsWith("signature=")) return trimmed.substring(10).trim();
        if (lower.startsWith("v1=")) return trimmed.substring(3).trim();
        return trimmed;
    }

    private String buildPayload(PaymentWebhookRequest request) {
        if (hasText(request.getRawPayload())) {
            return request.getRawPayload().trim();
        }
        return String.join("|",
                safe(request.getProvider()),
                safe(request.getOrderNumber()),
                safe(request.getPaymentStatus()),
                safe(request.getEventId()),
                safe(request.getTransactionId()),
                safe(request.getResponseCode()));
    }

    private byte[] computeHmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new UnauthorizedException("Unable to verify webhook signature");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean safeEquals(String left, String right) {
        if (!hasText(left) || !hasText(right)) return false;
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) return value.trim();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
