package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.services.dashboard.DashboardService;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.web.exceptions.ExportException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.hoz.hozitech.application.services.export.ExportHelpers.*;

/**
 * Generates revenue report as multi-sheet Excel workbook.
 */
@Component
@RequiredArgsConstructor
public class RevenueReportExporter {

    private final DashboardService dashboardService;

    public byte[] exportRevenueReport(String period) {
        DashboardStatsResponse stats = dashboardService.getDashboardStats(period);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFontPoi = workbook.createFont();
            titleFontPoi.setBold(true);
            titleFontPoi.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFontPoi);

            // Sheet 1: Doanh thu theo thời gian
            Sheet revenueSheet = workbook.createSheet("Doanh thu");
            Row titleRow = revenueSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DOANH THU - " + period);
            titleCell.setCellStyle(titleStyle);

            revenueSheet.createRow(1);

            String[] revHeaders = {"Thời gian", "Số đơn hàng", "Doanh thu (VNĐ)"};
            Row revHeaderRow = revenueSheet.createRow(2);
            for (int i = 0; i < revHeaders.length; i++) {
                Cell cell = revHeaderRow.createCell(i);
                cell.setCellValue(revHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            if (stats.getRevenueChart() != null) {
                for (DashboardStatsResponse.RevenueChartItem item : stats.getRevenueChart()) {
                    Row row = revenueSheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getLabel());
                    row.createCell(1).setCellValue(item.getOrders());
                    Cell revCell = row.createCell(2);
                    revCell.setCellValue(item.getRevenue() != null ? item.getRevenue().doubleValue() : 0);
                    revCell.setCellStyle(currencyStyle);
                }
            }
            Row summaryRow = revenueSheet.createRow(rowIdx + 1);
            summaryRow.createCell(0).setCellValue("TỔNG CỘNG");
            summaryRow.createCell(1).setCellValue(stats.getTotalOrders());
            Cell totalRevCell = summaryRow.createCell(2);
            totalRevCell.setCellValue(stats.getTotalRevenue() != null ? stats.getTotalRevenue().doubleValue() : 0);
            totalRevCell.setCellStyle(currencyStyle);

            for (int i = 0; i < revHeaders.length; i++) revenueSheet.autoSizeColumn(i);

            // Sheet 2: Top sản phẩm
            Sheet prodSheet = workbook.createSheet("Top sản phẩm");
            Row prodTitle = prodSheet.createRow(0);
            prodTitle.createCell(0).setCellValue("TOP SẢN PHẨM BÁN CHẠY");
            prodTitle.getCell(0).setCellStyle(titleStyle);

            String[] prodHeaders = {"#", "Sản phẩm", "Đã bán", "Doanh thu (VNĐ)"};
            Row prodHeaderRow = prodSheet.createRow(2);
            for (int i = 0; i < prodHeaders.length; i++) {
                Cell cell = prodHeaderRow.createCell(i);
                cell.setCellValue(prodHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int pIdx = 3;
            if (stats.getTopProducts() != null) {
                int rank = 1;
                for (DashboardStatsResponse.TopProductItem p : stats.getTopProducts()) {
                    Row row = prodSheet.createRow(pIdx++);
                    row.createCell(0).setCellValue(rank++);
                    row.createCell(1).setCellValue(p.getName());
                    row.createCell(2).setCellValue(p.getTotalSold());
                    Cell pRevCell = row.createCell(3);
                    pRevCell.setCellValue(p.getRevenue() != null ? p.getRevenue().doubleValue() : 0);
                    pRevCell.setCellStyle(currencyStyle);
                }
            }
            for (int i = 0; i < prodHeaders.length; i++) prodSheet.autoSizeColumn(i);

            // Sheet 3: Top khách hàng
            Sheet custSheet = workbook.createSheet("Top khách hàng");
            Row custTitle = custSheet.createRow(0);
            custTitle.createCell(0).setCellValue("TOP KHÁCH HÀNG TIỀM NĂNG");
            custTitle.getCell(0).setCellStyle(titleStyle);

            String[] custHeaders = {"#", "Khách hàng", "Email", "Số đơn", "Tổng chi tiêu (VNĐ)"};
            Row custHeaderRow = custSheet.createRow(2);
            for (int i = 0; i < custHeaders.length; i++) {
                Cell cell = custHeaderRow.createCell(i);
                cell.setCellValue(custHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int cIdx = 3;
            if (stats.getTopCustomers() != null) {
                int cRank = 1;
                for (DashboardStatsResponse.TopCustomerItem c : stats.getTopCustomers()) {
                    Row row = custSheet.createRow(cIdx++);
                    row.createCell(0).setCellValue(cRank++);
                    row.createCell(1).setCellValue(c.getName());
                    row.createCell(2).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                    row.createCell(3).setCellValue(c.getTotalOrders());
                    Cell cSpentCell = row.createCell(4);
                    cSpentCell.setCellValue(c.getTotalSpent() != null ? c.getTotalSpent().doubleValue() : 0);
                    cSpentCell.setCellStyle(currencyStyle);
                }
            }
            for (int i = 0; i < custHeaders.length; i++) custSheet.autoSizeColumn(i);

            // Sheet 4: Top danh mục
            Sheet catSheet = workbook.createSheet("Danh mục");
            Row catTitle = catSheet.createRow(0);
            catTitle.createCell(0).setCellValue("DOANH THU THEO DANH MỤC");
            catTitle.getCell(0).setCellStyle(titleStyle);

            String[] catHeaders = {"#", "Danh mục", "Đã bán", "Doanh thu (VNĐ)"};
            Row catHeaderRow = catSheet.createRow(2);
            for (int i = 0; i < catHeaders.length; i++) {
                Cell cell = catHeaderRow.createCell(i);
                cell.setCellValue(catHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int catIdx = 3;
            if (stats.getTopCategories() != null) {
                int catRank = 1;
                for (DashboardStatsResponse.TopCategoryItem cat : stats.getTopCategories()) {
                    Row row = catSheet.createRow(catIdx++);
                    row.createCell(0).setCellValue(catRank++);
                    row.createCell(1).setCellValue(cat.getName());
                    row.createCell(2).setCellValue(cat.getTotalSold());
                    Cell catRevCell = row.createCell(3);
                    catRevCell.setCellValue(cat.getRevenue() != null ? cat.getRevenue().doubleValue() : 0);
                    catRevCell.setCellStyle(currencyStyle);
                }
            }
            for (int i = 0; i < catHeaders.length; i++) catSheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export revenue report", e);
        }
    }
}
