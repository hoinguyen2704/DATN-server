package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscountType {
    PERCENTAGE("Phần trăm"),
    FIXED_AMOUNT("Số tiền cố định"),
    FREE_SHIP("Miễn phí vận chuyển");

    private final String description;
}
