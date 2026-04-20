package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.config.exceptions.InvalidParamException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class ReportRangeResolver {

    private ReportRangeResolver() {
    }

    public static ReportDateRange resolve(
            ReportRangeMode mode,
            LocalDate fromDate,
            LocalDate toDate,
            String month,
            Integer year
    ) {
        if (mode == null) {
            throw new InvalidParamException("Mode is required");
        }

        return switch (mode) {
            case CUSTOM -> resolveCustom(fromDate, toDate);
            case MONTH -> resolveMonth(month);
            case YEAR -> resolveYear(year);
        };
    }

    private static ReportDateRange resolveCustom(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidParamException("fromDate and toDate are required when mode=CUSTOM");
        }
        if (fromDate.isAfter(toDate)) {
            throw new InvalidParamException("fromDate must be before or equal to toDate");
        }

        return new ReportDateRange(
                ReportRangeMode.CUSTOM,
                fromDate.atStartOfDay(),
                toDate.atTime(LocalTime.MAX),
                fromDate + " đến " + toDate,
                fromDate + "_" + toDate
        );
    }

    private static ReportDateRange resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new InvalidParamException("month is required when mode=MONTH");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(month);
            LocalDate start = yearMonth.atDay(1);
            LocalDate end = yearMonth.atEndOfMonth();
            return new ReportDateRange(
                    ReportRangeMode.MONTH,
                    start.atStartOfDay(),
                    end.atTime(LocalTime.MAX),
                    "Tháng " + month,
                    month
            );
        } catch (DateTimeParseException ex) {
            throw new InvalidParamException("month must be in YYYY-MM format");
        }
    }

    private static ReportDateRange resolveYear(Integer year) {
        if (year == null) {
            throw new InvalidParamException("year is required when mode=YEAR");
        }

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return new ReportDateRange(
                ReportRangeMode.YEAR,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX),
                "Năm " + year,
                String.valueOf(year)
        );
    }
}
