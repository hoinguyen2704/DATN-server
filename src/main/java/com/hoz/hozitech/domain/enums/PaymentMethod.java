package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"),
    VNPAY("VNPay"),
    MOMO("MoMo"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    private final String description;
}
