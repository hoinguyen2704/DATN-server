package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FlashSaleStatus {
    SCHEDULED("Sắp diễn ra"),
    ACTIVE("Đang diễn ra"),
    ENDED("Đã kết thúc"),
    HIDDEN("Đã ẩn");

    private final String description;
}
