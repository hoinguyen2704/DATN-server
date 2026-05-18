package com.hoz.hozitech.application.services.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoz.hozitech.application.config.payment.MomoProperties;
import com.hoz.hozitech.config.exceptions.UnauthorizedException;
import com.hoz.hozitech.domain.dtos.request.MomoIpnRequest;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.web.exceptions.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final MomoProperties momoProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String createPaymentUrl(Order order) {
        ensureCreateConfiguration();

        String orderId = order.getOrderNumber();
        String requestId = orderId + "-" + System.currentTimeMillis();
        long amount = order.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        String orderInfo = "Thanh toan don hang " + orderId;
        String extraData = "";

        String rawData = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + momoProperties.getRequestType();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", momoProperties.getPartnerCode());
        body.put("requestType", momoProperties.getRequestType());
        body.put("ipnUrl", momoProperties.getIpnUrl());
        body.put("redirectUrl", momoProperties.getRedirectUrl());
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("orderInfo", orderInfo);
        body.put("requestId", requestId);
        body.put("extraData", extraData);
        body.put("lang", "vi");
        body.put("autoCapture", momoProperties.isAutoCapture());
        body.put("signature", hmacSha256(rawData, momoProperties.getSecretKey()));

        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeEndpoint() + momoProperties.getCreatePath()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("momo_create_http_failed orderNumber={} status={}", orderId, response.statusCode());
                throw paymentGatewayException("MoMo payment gateway returned HTTP " + response.statusCode());
            }

            MomoCreateResponse momoResponse = objectMapper.readValue(response.body(), MomoCreateResponse.class);
            if (momoResponse.getResultCode() == null || momoResponse.getResultCode() != 0 || !hasText(momoResponse.getPayUrl())) {
                log.warn("momo_create_failed orderNumber={} resultCode={} message={}",
                        orderId, momoResponse.getResultCode(), momoResponse.getMessage());
                throw paymentGatewayException("MoMo payment gateway rejected payment creation");
            }

            return momoResponse.getPayUrl();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("momo_create_exception orderNumber={}", orderId, ex);
            throw paymentGatewayException("Unable to create MoMo payment URL");
        }
    }

    public void verifyIpnSignatureOrThrow(MomoIpnRequest request) {
        ensureSecretConfigured();
        if (!hasText(request.getSignature())) {
            throw new UnauthorizedException("Missing MoMo IPN signature");
        }
        if (!momoProperties.getPartnerCode().equals(safe(request.getPartnerCode()))) {
            throw new UnauthorizedException("MoMo IPN partnerCode does not match configured merchant");
        }

        String rawData = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + safe(request.getAmount())
                + "&extraData=" + safe(request.getExtraData())
                + "&message=" + safe(request.getMessage())
                + "&orderId=" + safe(request.getOrderId())
                + "&orderInfo=" + safe(request.getOrderInfo())
                + "&orderType=" + safe(request.getOrderType())
                + "&partnerCode=" + safe(request.getPartnerCode())
                + "&payType=" + safe(request.getPayType())
                + "&requestId=" + safe(request.getRequestId())
                + "&responseTime=" + safe(request.getResponseTime())
                + "&resultCode=" + safe(request.getResultCode())
                + "&transId=" + safe(request.getTransId());

        String expectedSignature = hmacSha256(rawData, momoProperties.getSecretKey());
        if (!safeEqualsHex(request.getSignature(), expectedSignature)) {
            throw new UnauthorizedException("Invalid MoMo IPN signature");
        }
    }

    private void ensureCreateConfiguration() {
        if (!hasText(momoProperties.getPartnerCode())
                || !hasText(momoProperties.getAccessKey())
                || !hasText(momoProperties.getSecretKey())
                || !hasText(momoProperties.getEndpoint())
                || !hasText(momoProperties.getCreatePath())
                || !hasText(momoProperties.getRedirectUrl())
                || !hasText(momoProperties.getIpnUrl())
                || !hasText(momoProperties.getRequestType())) {
            throw paymentGatewayException("MoMo payment configuration is incomplete");
        }
    }

    private void ensureSecretConfigured() {
        if (!hasText(momoProperties.getAccessKey()) || !hasText(momoProperties.getSecretKey())) {
            throw new UnauthorizedException("MoMo payment configuration is incomplete");
        }
    }

    private BusinessException paymentGatewayException(String message) {
        return new BusinessException(BusinessErrorCode.PAYMENT_GATEWAY_ERROR, message, HttpStatus.BAD_GATEWAY);
    }

    private String normalizeEndpoint() {
        String endpoint = momoProperties.getEndpoint().trim();
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private String hmacSha256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw paymentGatewayException("Unable to sign MoMo payment request");
        }
    }

    private boolean safeEqualsHex(String provided, String expected) {
        if (!hasText(provided) || !hasText(expected)) return false;
        return MessageDigest.isEqual(
                provided.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                expected.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MomoCreateResponse {
        private String partnerCode;
        private String requestId;
        private String orderId;
        private Long amount;
        private Long responseTime;
        private String message;
        private Integer resultCode;
        private String payUrl;
        private String deeplink;
        private String qrCodeUrl;
        private String signature;
    }
}
