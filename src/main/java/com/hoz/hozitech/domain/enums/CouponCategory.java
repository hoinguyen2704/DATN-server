package com.hoz.hozitech.domain.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponCategory {
    PRODUCT("Giảm giá sản phẩm"),
    SHIPPING("Giảm giá vận chuyển");

    private final String description;
}
