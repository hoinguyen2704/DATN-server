package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponResponse {
    private UUID id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;

    // ─── NEW ─────────────────────────────────────────
    private Boolean isPublic;
    private String applyType;
    private List<ApplicableProductInfo> applicableProducts;
    private Boolean saved; // user đã lưu voucher này chưa (chỉ dùng ở public API)

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApplicableProductInfo {
        private UUID id;
        private String name;
        private String slug;
        private String mainImageUrl;
    }
}
