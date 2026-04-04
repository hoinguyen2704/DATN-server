package com.hoz.hozitech.application.config;

import com.hoz.hozitech.application.repositories.SettingRepository;
import com.hoz.hozitech.domain.entities.Setting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SettingDataSeeder implements ApplicationRunner {

    private final SettingRepository settingRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (settingRepository.count() > 0) {
            log.info("Settings already seeded, skipping...");
            return;
        }

        log.info("Seeding default settings...");

        List<Setting> defaults = List.of(
                // ─── SHOP ────────────────────────────────────────────
                buildSetting("SHOP", "SHOP_NAME", "Hozitech", "STRING", "Tên cửa hàng"),
                buildSetting("SHOP", "SHOP_EMAIL", "hozinium@gmail.com", "STRING", "Email cửa hàng"),
                buildSetting("SHOP", "SUPPORT_EMAIL", "hozinium@gmail.com", "STRING", "Email hỗ trợ khách hàng"),
                buildSetting("SHOP", "HOTLINE", "0828443833", "STRING", "Số điện thoại hotline"),
                buildSetting("SHOP", "SHOP_ADDRESS", "132 Cầu Diễn, Hà Nội", "STRING", "Địa chỉ cửa hàng"),
                buildSetting("SHOP", "CURRENCY", "VND", "STRING", "Đơn vị tiền tệ"),
                buildSetting("SHOP", "DEFAULT_TAX_PERCENT", "10", "NUMBER", "Thuế mặc định (%)"),

                // ─── PAYMENT ─────────────────────────────────────────
                buildSetting("PAYMENT", "COD_ENABLED", "true", "BOOLEAN", "Thanh toán khi nhận hàng"),
                buildSetting("PAYMENT", "VNPAY_ENABLED", "true", "BOOLEAN", "Thanh toán qua VNPay"),
                buildSetting("PAYMENT", "MOMO_ENABLED", "false", "BOOLEAN", "Thanh toán qua MoMo"),
                buildSetting("PAYMENT", "BANK_TRANSFER_ENABLED", "false", "BOOLEAN", "Chuyển khoản ngân hàng"),

                // ─── SHIPPING ────────────────────────────────────────
                buildSetting("SHIPPING", "DEFAULT_SHIPPING_FEE", "30000", "NUMBER", "Phí vận chuyển mặc định (VNĐ)"),
                buildSetting("SHIPPING", "FREE_SHIPPING_THRESHOLD", "500000", "NUMBER", "Ngưỡng miễn phí vận chuyển (VNĐ)"),

                // ─── AI ──────────────────────────────────────────────
                buildSetting("AI", "RECOMMENDATION_ENABLED", "true", "BOOLEAN", "Bật gợi ý sản phẩm AI"),
                buildSetting("AI", "AI_CONTENT_ENABLED", "false", "BOOLEAN", "Bật tạo nội dung bằng AI")
        );

        settingRepository.saveAll(defaults);
        log.info("Seeded {} default settings successfully.", defaults.size());
    }

    private Setting buildSetting(String group, String key, String value, String type, String desc) {
        return Setting.builder()
                .groupName(group)
                .settingKey(key)
                .settingValue(value)
                .valueType(type)
                .description(desc)
                .build();
    }
}
