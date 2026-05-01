package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.web.base.RestAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestAPI("${api.prefix-client}/settings")
@RequiredArgsConstructor
public class PublicSettingController {

    private final SettingService settingService;
    private final LocalizedApiResponseFactory responseFactory;

    /** Thông tin cửa hàng — dùng cho header, footer, trang liên hệ */
    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<Map<String, String>>> getShopInfo() {
        Map<String, String> shop = new LinkedHashMap<>();
        shop.put("shopName", val("SHOP_NAME"));
        shop.put("shopEmail", val("SHOP_EMAIL"));
        shop.put("supportEmail", val("SUPPORT_EMAIL"));
        shop.put("hotline", val("HOTLINE"));
        shop.put("address", val("SHOP_ADDRESS"));
        shop.put("currency", val("CURRENCY"));
        return ResponseEntity.ok(responseFactory.success("response.setting.shop_info_fetched", shop));
    }

    /** Danh sách payment methods đang bật — dùng cho Checkout */
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getPaymentMethods() {
        var methods = new java.util.ArrayList<Map<String, Object>>();

        addIfEnabled(methods, "COD", "Thanh toán khi nhận hàng", "COD_ENABLED");
        addIfEnabled(methods, "VNPAY", "VNPay", "VNPAY_ENABLED");
        addIfEnabled(methods, "MOMO", "MoMo", "MOMO_ENABLED");
        addIfEnabled(methods, "BANK_TRANSFER", "Chuyển khoản ngân hàng", "BANK_TRANSFER_ENABLED");

        return ResponseEntity.ok(responseFactory.success("response.setting.payment_methods_fetched", methods));
    }

    /** Cấu hình vận chuyển — dùng cho Checkout tính phí ship */
    @GetMapping("/shipping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShippingConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("defaultShippingFee", settingService.getSettingNumber("DEFAULT_SHIPPING_FEE"));
        config.put("freeShippingThreshold", settingService.getSettingNumber("FREE_SHIPPING_THRESHOLD"));
        return ResponseEntity.ok(responseFactory.success("response.setting.shipping_config_fetched", config));
    }

    /** Cấu hình thuế — dùng cho Checkout hiển thị breakdown thuế */
    @GetMapping("/tax")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaxConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", bool("TAX_ENABLED", true));
        config.put("taxPercent", num("DEFAULT_TAX_PERCENT", BigDecimal.TEN));

        String taxMode = valOrDefault("TAX_MODE", "INCLUDED").toUpperCase();
        if (!"EXCLUDED".equals(taxMode))
            taxMode = "INCLUDED";
        config.put("taxMode", taxMode);

        config.put("applyOnShipping", bool("TAX_APPLY_ON_SHIPPING", true));
        return ResponseEntity.ok(responseFactory.success("response.setting.tax_config_fetched", config));
    }

    // Helpers

    private String val(String key) {
        String v = settingService.getSettingValue(key);
        return v != null ? v : "";
    }

    private String valOrDefault(String key, String fallback) {
        String v = settingService.getSettingValue(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private boolean bool(String key, boolean fallback) {
        String v = settingService.getSettingValue(key);
        return (v == null || v.isBlank()) ? fallback : "true".equalsIgnoreCase(v);
    }

    private BigDecimal num(String key, BigDecimal fallback) {
        String v = settingService.getSettingValue(key);
        if (v == null || v.isBlank())
            return fallback;
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void addIfEnabled(java.util.List<Map<String, Object>> list,
            String id, String label, String enabledKey) {
        boolean enabled = settingService.getSettingBoolean(enabledKey);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("enabled", enabled);
        list.add(m);
    }
}
