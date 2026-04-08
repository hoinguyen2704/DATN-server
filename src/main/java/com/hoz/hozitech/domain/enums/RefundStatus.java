package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {
    PENDING("Chờ hoàn tiền"),
    PROCESSING("Đang xử lý"),
    SUCCESS("Hoàn tiền thành công"),
    FAILED("Hoàn tiền thất bại"),
    REVERSED("Hoàn tiền bị đảo ngược");

    private final String description;
}
