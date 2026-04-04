package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    DRAFT("Bản nháp");

    private final String description;
}
