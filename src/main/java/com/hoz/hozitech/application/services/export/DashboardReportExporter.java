package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.services.dashboard.DashboardService;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.web.exceptions.ExportException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.hoz.hozitech.application.services.export.ExportHelpers.*;

/**
 * Generates dashboard PDF reports for various report types
 * (orders, revenue, products, customers, returns, reviews).
 */
@Component
@RequiredArgsConstructor
public class DashboardReportExporter {

    private final DashboardService dashboardService;

    public byte[] exportDashboardReportPdf(String reportType, String period) {
        DashboardStatsResponse stats = dashboardService.getDashboardStats(period);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = loadVietnameseFont();
            Font headerFont = new Font(baseFont, 10, Font.BOLD, Color.WHITE);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
            Font boldFont = new Font(baseFont, 10, Font.BOLD, Color.BLACK);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL, Color.GRAY);
            Font greenBoldFont = new Font(baseFont, 10, Font.BOLD, new Color(5, 150, 105));
            Font purpleBoldFont = new Font(baseFont, 10, Font.BOLD, new Color(147, 51, 234));

            addReportHeader(document, baseFont, reportType, period);

            Color altRowColor = new Color(248, 250, 252);

            switch (reportType.toLowerCase()) {
                case "orders":
                    buildOrdersReportPdf(document, stats, headerFont, normalFont, boldFont, smallFont, altRowColor);
                    break;
                case "revenue":
                    buildRevenueReportPdf(document, stats, headerFont, normalFont, boldFont, greenBoldFont, altRowColor);
                    break;
                case "products":
                    buildProductsReportPdf(document, stats, headerFont, normalFont, boldFont, greenBoldFont, altRowColor);
                    break;
                case "customers":
                    buildCustomersReportPdf(document, stats, headerFont, normalFont, boldFont, purpleBoldFont, altRowColor);
                    break;
                case "returns":
                    buildReturnsReportPdf(document, stats, baseFont);
                    break;
                case "reviews":
                    buildReviewsReportPdf(document, stats, baseFont, normalFont, boldFont);
                    break;
                default:
                    document.add(new Paragraph("Loại báo cáo không hợp lệ: " + reportType, normalFont));
            }

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Hệ thống HoziTech - Báo cáo được tạo tự động", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new ExportException("Failed to generate dashboard report PDF: " + reportType, e);
        }
    }

    // ─── Report Header ───

    private void addReportHeader(Document document, BaseFont baseFont, String reportType, String period) throws Exception {
        Font logoFont = new Font(baseFont, 18, Font.BOLD, PRIMARY_COLOR);
        Font titleFont = new Font(baseFont, 16, Font.BOLD, Color.BLACK);
        Font subtitleFont = new Font(baseFont, 10, Font.NORMAL, Color.GRAY);

        String periodLabel;
        switch (period.toUpperCase()) {
            case "WEEK": periodLabel = "Theo tuần"; break;
            case "MONTH": periodLabel = "Theo tháng"; break;
            case "YEAR": periodLabel = "Theo năm"; break;
            default: periodLabel = period;
        }

        String reportTitle;
        switch (reportType.toLowerCase()) {
            case "orders": reportTitle = "BÁO CÁO ĐƠN HÀNG GẦN ĐÂY"; break;
            case "revenue": reportTitle = "BÁO CÁO DOANH THU"; break;
            case "products": reportTitle = "BÁO CÁO SẢN PHẨM BÁN CHẠY"; break;
            case "customers": reportTitle = "BÁO CÁO KHÁCH HÀNG TIỀM NĂNG"; break;
            case "returns": reportTitle = "BÁO CÁO TỈ LỆ HOÀN / HỦY"; break;
            case "reviews": reportTitle = "BÁO CÁO TỔNG QUAN ĐÁNH GIÁ"; break;
            default: reportTitle = "BÁO CÁO";
        }

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{50, 50});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("HoziTech", logoFont));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph datePara = new Paragraph(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")),
                subtitleFont);
        datePara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(datePara);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderColor(new Color(226, 232, 240));
        lineCell.setBorderWidth(2f);
        lineCell.setFixedHeight(8);
        line.addCell(lineCell);
        document.add(line);
        document.add(new Paragraph(" "));

        Paragraph title = new Paragraph(reportTitle, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + periodLabel,
                subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        document.add(new Paragraph(" "));
    }

    // ─── Orders Report ───

    private void buildOrdersReportPdf(Document document, DashboardStatsResponse stats,
                                       Font headerFont, Font normalFont, Font boldFont, Font smallFont, Color altRowColor) throws Exception {
        if (stats.getRecentOrders() == null || stats.getRecentOrders().isEmpty()) {
            document.add(new Paragraph("Không có đơn hàng nào trong khoảng thời gian này.", normalFont));
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{15, 25, 15, 20, 25});
        table.setSpacingBefore(10);

        String[] headers = {"Mã đơn", "Khách hàng", "Ngày đặt", "Tổng tiền", "Trạng thái"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
            hCell.setBackgroundColor(PRIMARY_COLOR);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        int idx = 0;
        for (DashboardStatsResponse.RecentOrderItem order : stats.getRecentOrders()) {
            Color bgColor = idx % 2 == 0 ? Color.WHITE : altRowColor;

            PdfPCell c1 = new PdfPCell(new Phrase(order.getOrderNumber(), boldFont));
            c1.setPadding(6); c1.setBackgroundColor(bgColor);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(order.getCustomerName() != null ? order.getCustomerName() : "N/A", normalFont));
            c2.setPadding(6); c2.setBackgroundColor(bgColor);
            table.addCell(c2);

            String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            PdfPCell c3 = new PdfPCell(new Phrase(dateStr, smallFont));
            c3.setPadding(6); c3.setBackgroundColor(bgColor); c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase(formatMoney(order.getTotalAmount()), boldFont));
            c4.setPadding(6); c4.setBackgroundColor(bgColor); c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c4);

            String statusStr = mapOrderStatusVi(order.getStatus());
            PdfPCell c5 = new PdfPCell(new Phrase(statusStr, normalFont));
            c5.setPadding(6); c5.setBackgroundColor(bgColor); c5.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c5);

            idx++;
        }

        document.add(table);

        document.add(new Paragraph(" "));
        Paragraph summary = new Paragraph("Tổng số đơn hiển thị: " + stats.getRecentOrders().size(), boldFont);
        summary.setAlignment(Element.ALIGN_RIGHT);
        document.add(summary);
    }

    // ─── Revenue Report ───

    private void buildRevenueReportPdf(Document document, DashboardStatsResponse stats,
                                        Font headerFont, Font normalFont, Font boldFont, Font greenBoldFont, Color altRowColor) throws Exception {
        if (stats.getRevenueChart() == null || stats.getRevenueChart().isEmpty()) {
            document.add(new Paragraph("Không có dữ liệu doanh thu.", normalFont));
            return;
        }

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 25, 40});
        table.setSpacingBefore(10);

        String[] headers = {"Thời gian", "Số đơn hàng", "Doanh thu"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
            hCell.setBackgroundColor(PRIMARY_COLOR);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        long totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int idx = 0;

        for (DashboardStatsResponse.RevenueChartItem item : stats.getRevenueChart()) {
            Color bgColor = idx % 2 == 0 ? Color.WHITE : altRowColor;

            PdfPCell c1 = new PdfPCell(new Phrase(item.getLabel(), boldFont));
            c1.setPadding(6); c1.setBackgroundColor(bgColor);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getOrders()), normalFont));
            c2.setPadding(6); c2.setBackgroundColor(bgColor); c2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(formatMoney(item.getRevenue()), greenBoldFont));
            c3.setPadding(6); c3.setBackgroundColor(bgColor); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c3);

            totalOrders += item.getOrders();
            if (item.getRevenue() != null) totalRevenue = totalRevenue.add(item.getRevenue());
            idx++;
        }

        PdfPCell tLabel = new PdfPCell(new Phrase("TỔNG CỘNG", boldFont));
        tLabel.setPadding(8); tLabel.setBackgroundColor(new Color(239, 246, 255));
        tLabel.setBorderWidth(2);
        table.addCell(tLabel);

        PdfPCell tOrders = new PdfPCell(new Phrase(String.valueOf(totalOrders), boldFont));
        tOrders.setPadding(8); tOrders.setBackgroundColor(new Color(239, 246, 255));
        tOrders.setHorizontalAlignment(Element.ALIGN_CENTER); tOrders.setBorderWidth(2);
        table.addCell(tOrders);

        PdfPCell tRev = new PdfPCell(new Phrase(formatMoney(totalRevenue), greenBoldFont));
        tRev.setPadding(8); tRev.setBackgroundColor(new Color(239, 246, 255));
        tRev.setHorizontalAlignment(Element.ALIGN_RIGHT); tRev.setBorderWidth(2);
        table.addCell(tRev);

        document.add(table);
    }

    // ─── Products Report ───

    private void buildProductsReportPdf(Document document, DashboardStatsResponse stats,
                                         Font headerFont, Font normalFont, Font boldFont, Font greenBoldFont, Color altRowColor) throws Exception {
        if (stats.getTopProducts() == null || stats.getTopProducts().isEmpty()) {
            document.add(new Paragraph("Không có dữ liệu sản phẩm.", normalFont));
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 47, 15, 30});
        table.setSpacingBefore(10);

        String[] headers = {"Top", "Sản phẩm", "Đã bán", "Doanh thu"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
            hCell.setBackgroundColor(PRIMARY_COLOR);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(h.equals("Sản phẩm") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        int rank = 1;
        for (DashboardStatsResponse.TopProductItem p : stats.getTopProducts()) {
            Color bgColor = rank % 2 != 0 ? Color.WHITE : altRowColor;

            PdfPCell c1 = new PdfPCell(new Phrase("#" + rank, boldFont));
            c1.setPadding(6); c1.setBackgroundColor(bgColor); c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(p.getName(), boldFont));
            c2.setPadding(6); c2.setBackgroundColor(bgColor);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(p.getTotalSold()), normalFont));
            c3.setPadding(6); c3.setBackgroundColor(bgColor); c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase(formatMoney(p.getRevenue()), greenBoldFont));
            c4.setPadding(6); c4.setBackgroundColor(bgColor); c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c4);

            rank++;
        }

        document.add(table);
    }

    // ─── Customers Report ───

    private void buildCustomersReportPdf(Document document, DashboardStatsResponse stats,
                                          Font headerFont, Font normalFont, Font boldFont, Font purpleBoldFont, Color altRowColor) throws Exception {
        if (stats.getTopCustomers() == null || stats.getTopCustomers().isEmpty()) {
            document.add(new Paragraph("Không có dữ liệu khách hàng.", normalFont));
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 25, 27, 12, 28});
        table.setSpacingBefore(10);

        String[] headers = {"Top", "Khách hàng", "Email", "Số đơn", "Tổng chi tiêu"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
            hCell.setBackgroundColor(PRIMARY_COLOR);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        int rank = 1;
        for (DashboardStatsResponse.TopCustomerItem c : stats.getTopCustomers()) {
            Color bgColor = rank % 2 != 0 ? Color.WHITE : altRowColor;

            PdfPCell c1 = new PdfPCell(new Phrase("#" + rank, boldFont));
            c1.setPadding(6); c1.setBackgroundColor(bgColor); c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase(c.getName(), boldFont));
            c2.setPadding(6); c2.setBackgroundColor(bgColor);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(c.getEmail() != null ? c.getEmail() : "", normalFont));
            c3.setPadding(6); c3.setBackgroundColor(bgColor);
            table.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase(String.valueOf(c.getTotalOrders()), normalFont));
            c4.setPadding(6); c4.setBackgroundColor(bgColor); c4.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c4);

            PdfPCell c5 = new PdfPCell(new Phrase(formatMoney(c.getTotalSpent()), purpleBoldFont));
            c5.setPadding(6); c5.setBackgroundColor(bgColor); c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c5);

            rank++;
        }

        document.add(table);
    }

    // ─── Returns Report ───

    private void buildReturnsReportPdf(Document document, DashboardStatsResponse stats, BaseFont baseFont) throws Exception {
        Font labelFont = new Font(baseFont, 12, Font.BOLD, new Color(220, 38, 38));
        Font bigRedFont = new Font(baseFont, 48, Font.BOLD, new Color(220, 38, 38));
        Font descFont = new Font(baseFont, 10, Font.NORMAL, new Color(220, 38, 38));
        Font orangeLabel = new Font(baseFont, 12, Font.BOLD, new Color(234, 88, 12));
        Font bigOrangeFont = new Font(baseFont, 48, Font.BOLD, new Color(234, 88, 12));
        Font orangeDesc = new Font(baseFont, 10, Font.NORMAL, new Color(234, 88, 12));

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(90);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingBefore(20);

        PdfPCell cancelCell = new PdfPCell();
        cancelCell.setBorder(Rectangle.BOX);
        cancelCell.setBorderColor(new Color(254, 202, 202));
        cancelCell.setBackgroundColor(new Color(254, 242, 242));
        cancelCell.setPadding(25);
        cancelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cancelCell.addElement(createCenteredParagraph("ĐƠN BỊ HỦY", labelFont));
        cancelCell.addElement(createCenteredParagraph(String.valueOf(stats.getCancelledOrders()), bigRedFont));
        cancelCell.addElement(createCenteredParagraph("Bao gồm đơn hủy và thanh toán thất bại", descFont));
        table.addCell(cancelCell);

        PdfPCell returnCell = new PdfPCell();
        returnCell.setBorder(Rectangle.BOX);
        returnCell.setBorderColor(new Color(254, 215, 170));
        returnCell.setBackgroundColor(new Color(255, 247, 237));
        returnCell.setPadding(25);
        returnCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        returnCell.addElement(createCenteredParagraph("YÊU CẦU HOÀN TRẢ", orangeLabel));
        returnCell.addElement(createCenteredParagraph(String.valueOf(stats.getReturnedOrders()), bigOrangeFont));
        returnCell.addElement(createCenteredParagraph("Yêu cầu trả hàng hoàn tiền từ khách hàng", orangeDesc));
        table.addCell(returnCell);

        document.add(table);
    }

    // ─── Reviews Report ───

    private void buildReviewsReportPdf(Document document, DashboardStatsResponse stats,
                                        BaseFont baseFont, Font normalFont, Font boldFont) throws Exception {
        Font bigYellowFont = new Font(baseFont, 48, Font.BOLD, new Color(234, 179, 8));
        Font starFont = new Font(baseFont, 20, Font.NORMAL, new Color(250, 204, 21));
        Font subtitleFont = new Font(baseFont, 10, Font.NORMAL, Color.GRAY);

        document.add(new Paragraph(" "));

        double avgRating = 5.0;
        if (stats.getTotalFeedbacks() > 0 && stats.getRatingDistribution() != null) {
            long totalScore = 0;
            for (var entry : stats.getRatingDistribution().entrySet()) {
                totalScore += entry.getKey() * entry.getValue();
            }
            avgRating = (double) totalScore / stats.getTotalFeedbacks();
        }

        Paragraph avgP = new Paragraph(String.format("%.1f", avgRating), bigYellowFont);
        avgP.setAlignment(Element.ALIGN_CENTER);
        document.add(avgP);

        Paragraph starsP = new Paragraph("★★★★★", starFont);
        starsP.setAlignment(Element.ALIGN_CENTER);
        document.add(starsP);

        Paragraph fromP = new Paragraph("Từ " + stats.getTotalFeedbacks() + " lượt đánh giá tổng hợp", subtitleFont);
        fromP.setAlignment(Element.ALIGN_CENTER);
        document.add(fromP);

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setWidths(new float[]{20, 50, 30});
        table.setSpacingBefore(15);

        for (int stars = 5; stars >= 1; stars--) {
            long count = stats.getRatingDistribution() != null ? stats.getRatingDistribution().getOrDefault(stars, 0L) : 0;
            long percent = stats.getTotalFeedbacks() > 0 ? Math.round((double) count / stats.getTotalFeedbacks() * 100) : 0;

            PdfPCell starCell = new PdfPCell(new Phrase(stars + " ★", boldFont));
            starCell.setBorder(Rectangle.NO_BORDER);
            starCell.setPadding(6);
            starCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(starCell);

            PdfPCell barCell = new PdfPCell(new Phrase(percent + "% (" + count + " lượt)", normalFont));
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setPadding(6);
            table.addCell(barCell);

            PdfPCell pctCell = new PdfPCell(new Phrase("█".repeat(Math.max(1, (int)(percent / 5))), new Font(baseFont, 10, Font.NORMAL, new Color(250, 204, 21))));
            pctCell.setBorder(Rectangle.NO_BORDER);
            pctCell.setPadding(6);
            table.addCell(pctCell);
        }

        document.add(table);
    }
}
