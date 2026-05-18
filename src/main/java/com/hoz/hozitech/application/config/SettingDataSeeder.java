package com.hoz.hozitech.application.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.repositories.SettingRepository;
import com.hoz.hozitech.domain.entities.Setting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SettingDataSeeder implements ApplicationRunner {

        private final SettingRepository settingRepository;
        private final JdbcTemplate jdbcTemplate;

        @Override
        public void run(ApplicationArguments args) {
                log.info("Ensuring default settings...");

                try {
                        // Force schema update for newly added columns if ddl-auto failed to add them
                        // (e.g. populated table with NOT NULL constraints)
                        jdbcTemplate.execute(
                                        "ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_percent numeric(5,2) default 0.00;");
                        jdbcTemplate
                                        .execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_mode varchar(20) default 'INCLUDED';");
                        jdbcTemplate
                                        .execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS taxable_amount numeric(15,2) default 0.00;");
                        jdbcTemplate.execute(
                                        "ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_amount numeric(15,2) default 0.00;");
                        jdbcTemplate.execute(
                                        "ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_apply_on_shipping boolean default false;");

                        // Drop NOT NULL constraint on user_id to allow guest tickets
                        jdbcTemplate.execute("ALTER TABLE tickets ALTER COLUMN user_id DROP NOT NULL;");
                } catch (Exception e) {
                        log.warn("Could not alter table orders automatically: {}", e.getMessage());
                }

                List<Setting> defaults = List.of(
                                // SHOP
                                buildSetting("SHOP", "SHOP_NAME", "Htech", "STRING", "Tên cửa hàng"),
                                buildSetting("SHOP", "SHOP_EMAIL", "hozinium@gmail.com", "STRING", "Email cửa hàng"),
                                buildSetting("SHOP", "SUPPORT_EMAIL", "hozinium@gmail.com", "STRING",
                                                "Email hỗ trợ khách hàng"),
                                buildSetting("SHOP", "HOTLINE", "0828443833", "STRING", "Số điện thoại hotline"),
                                buildSetting("SHOP", "SHOP_ADDRESS", "132 Cầu Diễn, Hà Nội", "STRING",
                                                "Địa chỉ cửa hàng"),
                                buildSetting("SHOP", "CURRENCY", "VND", "STRING", "Đơn vị tiền tệ"),
                                buildSetting("SHOP", "DEFAULT_TAX_PERCENT", "10", "NUMBER", "Thuế mặc định (%)"),
                                buildSetting("TAX", "TAX_ENABLED", "true", "BOOLEAN", "Bật/tắt áp dụng thuế"),
                                buildSetting("TAX", "TAX_MODE", "INCLUDED", "STRING",
                                                "Chế độ thuế: INCLUDED | EXCLUDED"),
                                buildSetting("TAX", "TAX_APPLY_ON_SHIPPING", "true", "BOOLEAN",
                                                "Có áp thuế lên phí vận chuyển hay không"),

                                // PAYMENT
                                buildSetting("PAYMENT", "COD_ENABLED", "true", "BOOLEAN", "Thanh toán khi nhận hàng"),
                                buildSetting("PAYMENT", "VNPAY_ENABLED", "true", "BOOLEAN", "Thanh toán qua VNPay"),
                                buildSetting("PAYMENT", "MOMO_ENABLED", "true", "BOOLEAN", "Thanh toán qua MoMo"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_ENABLED", "false", "BOOLEAN",
                                                "Chuyển khoản ngân hàng"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_BANK_NAME", "", "STRING",
                                                "Tên ngân hàng nhận chuyển khoản"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_ACCOUNT_NUMBER", "", "STRING",
                                                "Số tài khoản nhận chuyển khoản"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_ACCOUNT_NAME", "", "STRING",
                                                "Tên chủ tài khoản nhận chuyển khoản"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_QR_IMAGE_URL", "", "STRING",
                                                "Ảnh QR chuyển khoản"),
                                buildSetting("PAYMENT", "BANK_TRANSFER_INSTRUCTIONS",
                                                "Đơn hàng sẽ được xác nhận sau khi shop kiểm tra giao dịch chuyển khoản.",
                                                "STRING", "Ghi chú hướng dẫn chuyển khoản"),

                                // SHIPPING
                                buildSetting("SHIPPING", "DEFAULT_SHIPPING_FEE", "30000", "NUMBER",
                                                "Phí vận chuyển mặc định (VNĐ)"),
                                buildSetting("SHIPPING", "FREE_SHIPPING_THRESHOLD", "500000", "NUMBER",
                                                "Ngưỡng miễn phí vận chuyển (VNĐ)"),

                                // AI
                                buildSetting("AI", "RECOMMENDATION_ENABLED", "true", "BOOLEAN",
                                                "Bật gợi ý sản phẩm AI"),
                                buildSetting("AI", "AI_CONTENT_ENABLED", "false", "BOOLEAN",
                                                "Bật tạo nội dung bằng AI"));
                int created = 0;
                for (Setting setting : defaults) {
                        if (!settingRepository.existsBySettingKey(setting.getSettingKey())) {
                                settingRepository.save(setting);
                                created++;
                        }
                }
                log.info("Default settings ensured. Added {} missing keys (total defaults = {}).", created,
                                defaults.size());
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
