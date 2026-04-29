package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CouponRequest {

    @NotBlank(message = "{validation.coupon_code_is_required}")
    private String code;

    @NotBlank(message = "{validation.discount_type_is_required}")
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    private String couponCategory; // PRODUCT, SHIPPING

    @NotNull(message = "{validation.discount_value_is_required}")
    @Min(0)
    private BigDecimal discountValue;

    @Min(0)
    private BigDecimal minOrderValue;

    @Min(0)
    private BigDecimal maxDiscountAmount;

    @Min(1)
    private Integer usageLimit;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // NEW
    private Boolean isPublic; // true = hiển thị trên storefront
    private String applyType; // ALL | SPECIFIC_PRODUCTS
    private List<UUID> applicableProductIds; // danh sách SP áp dụng
}
