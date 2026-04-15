package com.hoz.hozitech.application.services.export;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Facade service that delegates export operations to specialized sub-exporters.
 *
 * Refactored from a 1,331-line God Class into a thin delegation layer.
 * Sub-exporters:
 * - {@link InvoicePdfExporter} — Single order invoice PDF
 * - {@link RevenueReportExporter} — Revenue summary Excel (multi-sheet)
 * - {@link DashboardReportExporter} — Dashboard PDF reports (7 types)
 * - {@link DataExcelExporter} — Data exports (Orders/Users/Feedbacks/Products Excel)
 * - {@link ExportHelpers} — Shared PDF/Excel utility methods
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final InvoicePdfExporter invoicePdfExporter;
    private final RevenueReportExporter revenueReportExporter;
    private final DashboardReportExporter dashboardReportExporter;
    private final DataExcelExporter dataExcelExporter;

    @Override
    public byte[] exportOrderInvoicePdf(UUID orderId) {
        return invoicePdfExporter.exportOrderInvoicePdf(orderId);
    }

    @Override
    public byte[] exportRevenueReport(String period) {
        return revenueReportExporter.exportRevenueReport(period);
    }

    @Override
    public byte[] exportDashboardReportPdf(String reportType, String period) {
        return dashboardReportExporter.exportDashboardReportPdf(reportType, period);
    }

    @Override
    public byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        return dataExcelExporter.exportOrdersToExcel(status, keyword, from, to);
    }

    @Override
    public byte[] exportUsersToExcel(String keyword, String role) {
        return dataExcelExporter.exportUsersToExcel(keyword, role);
    }

    @Override
    public byte[] exportFeedbacksToExcel(String status, UUID productId) {
        return dataExcelExporter.exportFeedbacksToExcel(status, productId);
    }

    @Override
    public byte[] exportProductsToExcel(String keyword, UUID categoryId, String status) {
        return dataExcelExporter.exportProductsToExcel(keyword, categoryId, status);
    }

    @Override
    public byte[] exportReturnsToExcel(String status, String keyword) {
        return dataExcelExporter.exportReturnsToExcel(status, keyword);
    }
}
