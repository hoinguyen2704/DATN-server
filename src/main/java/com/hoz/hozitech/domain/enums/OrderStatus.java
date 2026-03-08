package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPING("Đang giao hàng"),
    SHIPPED("Đã giao hàng"),
    CANCELLED("Đã hủy"),
    RETURNED("Đã hoàn trả");

    private final String description;
}
