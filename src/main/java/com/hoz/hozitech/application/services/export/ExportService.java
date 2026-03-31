package com.hoz.hozitech.application.services.export;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ExportService {

    byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to);

    byte[] exportUsersToExcel(String keyword, String role);

    byte[] exportFeedbacksToExcel(String status, UUID productId);

    /** Xuất danh sách sản phẩm dạng Excel (bao gồm số lượng đã bán) */
    byte[] exportProductsToExcel(String keyword, UUID categoryId, String status);

    /** Xuất hóa đơn chi tiết cho 1 đơn hàng dạng PDF */
    byte[] exportOrderInvoicePdf(UUID orderId);

    /** Xuất báo cáo doanh thu tổng hợp dạng Excel */
    byte[] exportRevenueReport(String period);

    /** Xuất báo cáo dashboard dạng PDF theo loại (orders, revenue, products, customers, returns, reviews) */
    byte[] exportDashboardReportPdf(String reportType, String period);
}
