package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    EXPIRED("Hết hạn"),
    PAUSED("Tạm dừng");

    private final String description;
}
