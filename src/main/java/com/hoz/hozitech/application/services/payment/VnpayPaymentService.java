package com.hoz.hozitech.application.services.payment;

import com.hoz.hozitech.application.config.payment.VnpayProperties;
import com.hoz.hozitech.domain.entities.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayPaymentService {

    private final VnpayProperties vnpayProperties;

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    public String createPaymentUrl(Order order, String ipAddress) {
        if (vnpayProperties.getTmnCode() == null || vnpayProperties.getTmnCode().isEmpty() ||
            vnpayProperties.getHashSecret() == null || vnpayProperties.getHashSecret().isEmpty()) {
            log.error("VNPAY configuration is missing (vnp_TmnCode or vnp_HashSecret)");
            return null;
        }

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpayProperties.getVersion());
        vnp_Params.put("vnp_Command", vnpayProperties.getCommand());
        vnp_Params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        // Amount needs to be multiplied by 100 per VNPAY's format
        long amount = order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        String txnRef = order.getOrderNumber();
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + txnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress != null && !ipAddress.isEmpty() ? ipAddress : "127.0.0.1");

        LocalDateTime createDate = LocalDateTime.now(VN_ZONE_ID);
        vnp_Params.put("vnp_CreateDate", FORMATTER.format(createDate));
        
        LocalDateTime expireDate = createDate.plusMinutes(vnpayProperties.getExpireMinutes());
        vnp_Params.put("vnp_ExpireDate", FORMATTER.format(expireDate));

        // Build data to hash and query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnpayProperties.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        return vnpayProperties.getPayUrl() + "?" + queryUrl;
    }

    public boolean verifyIpnSignature(Map<String, String> requestParams) {
        String vnp_SecureHash = requestParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
            return false;
        }

        // Copy map to remove hash params safely
        Map<String, String> fields = new HashMap<>(requestParams);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Hash all remaining fields. Spring automatically decodes the query parameters, 
        // so we MUST re-encode them to match VNPAY's original hash input string.
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }

        String signValue = hmacSHA512(vnpayProperties.getHashSecret(), hashData.toString());
        return signValue.equalsIgnoreCase(vnp_SecureHash);
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, HMAC_SHA512);
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Error when calculating HMAC SHA512", ex);
            return "";
        }
    }
}
