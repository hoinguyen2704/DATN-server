package com.hoz.hozitech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackStatus {
    APPROVED("Đã duyệt"),
    HIDDEN("Ẩn"),
    SPAM("Spam");

    private final String description;
}
