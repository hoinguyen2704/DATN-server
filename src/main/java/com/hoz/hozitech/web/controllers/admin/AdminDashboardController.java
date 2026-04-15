package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.dashboard.DashboardService;
import com.hoz.hozitech.application.services.export.ExportService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestAPI("${api.prefix-admin}/dashboard")
@RoleAdmin
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final ExportService exportService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @RequestParam(value = "period", defaultValue = "MONTH") String period) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully",
                dashboardService.getDashboardStats(period)));
    }

    @GetMapping("/top-variants")
    public ResponseEntity<ApiResponse<List<DashboardStatsResponse.TopVariantItem>>> getTopVariants(
            @RequestParam(value = "period", defaultValue = "MONTH") String period,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "Top variants fetched successfully",
                dashboardService.getTopVariants(period, limit)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam(value = "period", defaultValue = "MONTH") String period) {
        byte[] excelBytes = exportService.exportRevenueReport(period);

        String filename = "revenue_report_" + LocalDate.now() + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/report-pdf")
    public ResponseEntity<byte[]> exportDashboardReportPdf(
            @RequestParam(value = "type", defaultValue = "orders") String type,
            @RequestParam(value = "period", defaultValue = "MONTH") String period) {
        byte[] pdfBytes = exportService.exportDashboardReportPdf(type, period);

        String filename = "report_" + type + "_" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
