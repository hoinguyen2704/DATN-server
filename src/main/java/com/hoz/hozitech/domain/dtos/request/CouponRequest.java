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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotBlank(message = "Discount type is required")
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    @NotNull(message = "Discount value is required")
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
}
