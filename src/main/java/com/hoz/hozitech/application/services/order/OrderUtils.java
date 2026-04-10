package com.hoz.hozitech.application.services.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

//  Shared utility methods used by multiple Order/Return service classes.
//  Eliminates duplication of common helpers across OrderServiceImpl, ReturnServiceImpl, etc.

final class OrderUtils {

    static final int MONEY_SCALE = 2;
    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private OrderUtils() {}

    static BigDecimal money(BigDecimal value) {
        return value != null ? value.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO;
    }

    static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeIdempotencyKey(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) return null;
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized;
    }

    static String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        return String.format("%,.0f", price);
    }
}
