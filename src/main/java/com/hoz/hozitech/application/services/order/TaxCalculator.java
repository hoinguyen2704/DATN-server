package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.MONEY_SCALE;
import static com.hoz.hozitech.application.services.order.OrderUtils.ONE_HUNDRED;
import static com.hoz.hozitech.application.services.order.OrderUtils.nz;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.domain.enums.TaxMode;

import lombok.RequiredArgsConstructor;

//  Encapsulates all tax calculation logic.
//  Reads tax configuration from system settings and computes TaxSnapshot.

@Component
@RequiredArgsConstructor
class TaxCalculator {

    private final SettingService settingService;

    TaxSnapshot calculate(BigDecimal productBase, BigDecimal shippingBase) {
        BigDecimal safeProductBase = nz(productBase);
        BigDecimal safeShippingBase = nz(shippingBase);
        BigDecimal netTotal = safeProductBase.add(safeShippingBase);
        if (netTotal.compareTo(BigDecimal.ZERO) < 0) netTotal = BigDecimal.ZERO;

        boolean taxEnabled = boolSettingWithFallback("TAX_ENABLED", true);
        TaxMode taxMode = parseTaxMode(textSettingWithFallback("TAX_MODE", TaxMode.INCLUDED.name()));
        boolean taxApplyOnShipping = boolSettingWithFallback("TAX_APPLY_ON_SHIPPING", true);
        BigDecimal taxPercent = settingService.getSettingNumber("DEFAULT_TAX_PERCENT");
        if (taxPercent == null || taxPercent.compareTo(BigDecimal.ZERO) < 0) {
            taxPercent = BigDecimal.ZERO;
        }

        BigDecimal taxableAmount = safeProductBase;
        if (taxApplyOnShipping) {
            taxableAmount = taxableAmount.add(safeShippingBase);
        }
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) taxableAmount = BigDecimal.ZERO;

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = netTotal;

        if (taxEnabled && taxPercent.compareTo(BigDecimal.ZERO) > 0 && taxableAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (taxMode == TaxMode.EXCLUDED) {
                taxAmount = taxableAmount
                        .multiply(taxPercent)
                        .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
                totalAmount = netTotal.add(taxAmount);
            } else {
                BigDecimal denominator = ONE_HUNDRED.add(taxPercent);
                if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                    taxAmount = taxableAmount
                            .multiply(taxPercent)
                            .divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
                }
                totalAmount = netTotal;
            }
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        return new TaxSnapshot(taxPercent, taxMode, taxApplyOnShipping, taxableAmount, taxAmount, totalAmount);
    }

    private TaxMode parseTaxMode(String mode) {
        if (mode == null || mode.isBlank()) return TaxMode.INCLUDED;
        try {
            return TaxMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TaxMode.INCLUDED;
        }
    }

    private String textSettingWithFallback(String key, String fallback) {
        String value = settingService.getSettingValue(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private boolean boolSettingWithFallback(String key, boolean fallback) {
        String value = settingService.getSettingValue(key);
        if (value == null || value.isBlank()) return fallback;
        return "true".equalsIgnoreCase(value);
    }

    record TaxSnapshot(
            BigDecimal taxPercent,
            TaxMode taxMode,
            boolean taxApplyOnShipping,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {}
}
