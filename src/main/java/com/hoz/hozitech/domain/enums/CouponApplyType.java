package com.hoz.hozitech.domain.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponApplyType {
    ALL("Áp dụng cho tất cả sản phẩm"),
    SPECIFIC_PRODUCTS("Áp dụng cho sản phẩm cụ thể");

    private final String description;
}
