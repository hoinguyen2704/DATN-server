package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxMode {
    INCLUDED("Giá đã bao gồm thuế"),
    EXCLUDED("Thuế cộng thêm vào tổng");

    private final String description;
}
