package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReturnRequestStatus {
    REQUESTED("Yêu cầu trả hàng"),
    APPROVED("Đã duyệt"),
    REJECTED("Đã từ chối"),
    IN_TRANSIT("Đang gửi hàng hoàn"),
    RECEIVED("Đã nhận hàng hoàn"),
    QC_PASSED("QC đạt"),
    QC_FAILED("QC không đạt"),
    REFUND_PENDING("Chờ hoàn tiền"),
    REFUNDED("Đã hoàn tiền"),
    CANCELLED("Đã hủy yêu cầu"),
    CLOSED("Đã đóng");

    private final String description;
}
