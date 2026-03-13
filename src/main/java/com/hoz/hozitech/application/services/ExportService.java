package com.hoz.hozitech.application.services;

import java.time.LocalDateTime;

public interface ExportService {

    byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to);
}
