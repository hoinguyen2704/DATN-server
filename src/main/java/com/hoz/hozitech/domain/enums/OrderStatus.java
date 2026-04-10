package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("Đơn hàng đang chờ xác nhận", "chờ xác nhận"),
    CONFIRMED("Người gửi đang chuẩn bị hàng", "đã xác nhận"),
    PROCESSING("Đơn hàng đang được đóng gói", "đang xử lý"),
    SHIPPING("Đơn hàng đã được giao cho đơn vị vận chuyển", "đang giao"),
    SHIPPED("Đơn hàng đang trên đường giao đến bạn", "đã giao"),
    CANCELLED("Đơn hàng đã bị huỷ", "đã huỷ"),
    RETURNED("Đơn hàng đã được hoàn trả", "đã hoàn trả");

    private final String description;
    private final String label;
}
