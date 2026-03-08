package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    COMPLETED("Đã thanh toán"),
    FAILED("Thanh toán thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String description;
}
