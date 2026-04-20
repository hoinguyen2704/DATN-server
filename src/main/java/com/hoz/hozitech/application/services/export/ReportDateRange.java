package com.hoz.hozitech.application.services.export;

import java.time.LocalDateTime;

public record ReportDateRange(
        ReportRangeMode mode,
        LocalDateTime from,
        LocalDateTime to,
        String displayLabel,
        String fileLabel
) {
}
