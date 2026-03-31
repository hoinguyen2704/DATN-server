package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.dashboard.DashboardService;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.application.specifications.ProductSpecification;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final DashboardService dashboardService;
    private final ProductRepository productRepository;

    private static final String[] ORDER_HEADERS = {
            "Mã đơn", "Khách hàng", "Email", "SĐT",
            "Tạm tính", "Giảm giá", "Phí ship", "Thành tiền",
            "Trạng thái", "Thanh toán", "Ngày đặt"
    };

    private static final String[] USER_HEADERS = {
            "ID", "Họ tên", "Email", "SĐT", "Vai trò", "Ngày tạo"
    };

    private static final String[] FEEDBACK_HEADERS = {
            "ID", "Sản phẩm", "Khách hàng", "Email", "Đánh giá", "Nội dung",
            "Trạng thái", "Phản hồi admin", "Ngày tạo"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    private static final Color PRIMARY_COLOR = new Color(37, 57, 230); // #2539e6

    // ═══════════════════════════════════════════════════════════
    // INVOICE PDF
    // ═══════════════════════════════════════════════════════════

    @Override
    public byte[] exportOrderInvoicePdf(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = loadVietnameseFont();
            Font titleFont = new Font(baseFont, 22, Font.BOLD, PRIMARY_COLOR);
            Font headerFont = new Font(baseFont, 11, Font.BOLD, Color.WHITE);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
            Font boldFont = new Font(baseFont, 10, Font.BOLD, Color.BLACK);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL, Color.GRAY);
            Font bigBoldFont = new Font(baseFont, 13, Font.BOLD, PRIMARY_COLOR);

            // ═══ HEADER: Shop info + Invoice title ═══
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{55, 45});

            // Left: Shop info
            PdfPCell shopCell = new PdfPCell();
            shopCell.setBorder(Rectangle.NO_BORDER);
            shopCell.setPaddingBottom(15);
            Paragraph shopName = new Paragraph("HoziTech", titleFont);
            shopCell.addElement(shopName);
            shopCell.addElement(new Paragraph("123 Đường Công Nghệ, Quận IT, TP.HCM", smallFont));
            shopCell.addElement(new Paragraph("SĐT: 0123.456.789 | Email: contact@hozitech.com", smallFont));
            headerTable.addCell(shopCell);

            // Right: Invoice label
            PdfPCell invoiceCell = new PdfPCell();
            invoiceCell.setBorder(Rectangle.NO_BORDER);
            invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceCell.setPaddingBottom(15);
            Paragraph invoiceLabel = new Paragraph("HÓA ĐƠN", new Font(baseFont, 20, Font.BOLD, PRIMARY_COLOR));
            invoiceLabel.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(invoiceLabel);
            Paragraph orderNum = new Paragraph("#" + order.getOrderNumber(), new Font(baseFont, 14, Font.BOLD, Color.BLACK));
            orderNum.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(orderNum);
            String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "";
            Paragraph dateP = new Paragraph("Ngày: " + dateStr, smallFont);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(dateP);
            headerTable.addCell(invoiceCell);

            document.add(headerTable);

            // ═══ SEPARATOR LINE ═══
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(new Color(200, 200, 200));
            lineCell.setBorderWidth(1f);
            lineCell.setFixedHeight(5);
            line.addCell(lineCell);
            document.add(line);
            document.add(new Paragraph(" "));

            // ═══ CUSTOMER + PAYMENT INFO ═══
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            // Left: Customer
            PdfPCell custCell = new PdfPCell();
            custCell.setBorder(Rectangle.NO_BORDER);
            custCell.setPaddingBottom(10);
            custCell.addElement(new Paragraph("KHÁCH HÀNG", new Font(baseFont, 9, Font.BOLD, Color.GRAY)));
            String custName = order.getUser() != null ? order.getUser().getFullName() : "N/A";
            custCell.addElement(new Paragraph(custName, boldFont));
            String address = parseAddress(order.getShippingAddressJson());
            custCell.addElement(new Paragraph(address, normalFont));
            infoTable.addCell(custCell);

            // Right: Payment
            PdfPCell payCell = new PdfPCell();
            payCell.setBorder(Rectangle.NO_BORDER);
            payCell.setPaddingBottom(10);
            payCell.addElement(new Paragraph("THANH TOÁN & GIAO HÀNG", new Font(baseFont, 9, Font.BOLD, Color.GRAY)));
            payCell.addElement(new Paragraph("Phương thức: " + order.getPaymentMethod().name(), normalFont));
            String payStatus = order.getPaymentStatus() == PaymentStatus.COMPLETED ? "Đã thanh toán" : "Chưa thu tiền";
            payCell.addElement(new Paragraph("Trạng thái: " + payStatus, normalFont));
            if (order.getCouponCode() != null && !order.getCouponCode().isEmpty()) {
                payCell.addElement(new Paragraph("Mã giảm giá: " + order.getCouponCode(), boldFont));
            }
            infoTable.addCell(payCell);
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // ═══ ORDER ITEMS TABLE ═══
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{5, 40, 10, 20, 25});
            itemTable.setSpacingBefore(10);

            // Table header
            String[] tableHeaders = {"STT", "Sản phẩm", "SL", "Đơn giá", "Thành tiền"};
            for (String h : tableHeaders) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
                hCell.setBackgroundColor(PRIMARY_COLOR);
                hCell.setPadding(8);
                hCell.setHorizontalAlignment(h.equals("Sản phẩm") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                itemTable.addCell(hCell);
            }

            // Table rows
            int idx = 1;
            Color altRowColor = new Color(245, 245, 255);
            for (OrderItem item : order.getOrderItems()) {
                boolean isAlt = idx % 2 == 0;
                Color bgColor = isAlt ? altRowColor : Color.WHITE;

                PdfPCell sttCell = new PdfPCell(new Phrase(String.valueOf(idx), normalFont));
                sttCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                sttCell.setPadding(6);
                sttCell.setBackgroundColor(bgColor);
                itemTable.addCell(sttCell);

                // Product name + variant
                Phrase productPhrase = new Phrase();
                productPhrase.add(new Chunk(item.getProductName() + "\n", boldFont));
                if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
                    productPhrase.add(new Chunk("P/L: " + item.getVariantName(), smallFont));
                }
                PdfPCell prodCell = new PdfPCell(productPhrase);
                prodCell.setPadding(6);
                prodCell.setBackgroundColor(bgColor);
                itemTable.addCell(prodCell);

                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setPadding(6);
                qtyCell.setBackgroundColor(bgColor);
                itemTable.addCell(qtyCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(formatMoney(item.getUnitPrice()), normalFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setPadding(6);
                priceCell.setBackgroundColor(bgColor);
                itemTable.addCell(priceCell);

                PdfPCell subtotalCell = new PdfPCell(new Phrase(formatMoney(item.getSubtotal()), boldFont));
                subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                subtotalCell.setPadding(6);
                subtotalCell.setBackgroundColor(bgColor);
                itemTable.addCell(subtotalCell);

                idx++;
            }
            document.add(itemTable);

            // ═══ TOTALS ═══
            document.add(new Paragraph(" "));
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, "Tạm tính:", formatMoney(order.getSubtotal()), normalFont, boldFont);
            addTotalRow(totalsTable, "Phí vận chuyển:", formatMoney(order.getShippingFee()), normalFont, boldFont);
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                Font redFont = new Font(baseFont, 10, Font.BOLD, Color.RED);
                addTotalRow(totalsTable, "Giảm giá:", "-" + formatMoney(order.getDiscountAmount()), normalFont, redFont);
            }
            // Grand total
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TỔNG CỘNG:", bigBoldFont));
            totalLabelCell.setBorder(Rectangle.TOP);
            totalLabelCell.setBorderWidth(2);
            totalLabelCell.setPadding(8);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(totalLabelCell);
            PdfPCell totalValueCell = new PdfPCell(new Phrase(formatMoney(order.getTotalAmount()), bigBoldFont));
            totalValueCell.setBorder(Rectangle.TOP);
            totalValueCell.setBorderWidth(2);
            totalValueCell.setPadding(8);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(totalValueCell);

            document.add(totalsTable);

            // ═══ SIGNATURES ═══
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(80);
            sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell buyerSig = new PdfPCell();
            buyerSig.setBorder(Rectangle.NO_BORDER);
            buyerSig.setHorizontalAlignment(Element.ALIGN_CENTER);
            buyerSig.setPaddingTop(20);
            buyerSig.addElement(createCenteredParagraph("Người mua hàng", boldFont));
            buyerSig.addElement(createCenteredParagraph("(Ký, ghi rõ họ tên)", smallFont));
            sigTable.addCell(buyerSig);

            PdfPCell sellerSig = new PdfPCell();
            sellerSig.setBorder(Rectangle.NO_BORDER);
            sellerSig.setHorizontalAlignment(Element.ALIGN_CENTER);
            sellerSig.setPaddingTop(20);
            sellerSig.addElement(createCenteredParagraph("Người bán hàng", boldFont));
            sellerSig.addElement(createCenteredParagraph("(Ký, ghi rõ họ tên)", smallFont));
            sigTable.addCell(sellerSig);

            document.add(sigTable);

            // ═══ FOOTER ═══
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Cảm ơn quý khách đã mua sắm tại HoziTech!", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private BaseFont loadVietnameseFont() {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/OpenSans.ttf");
            String fontPath = fontResource.getFile().getAbsolutePath();
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                throw new RuntimeException("Cannot load font", ex);
            }
        }
    }

    private String parseAddress(String json) {
        if (json == null || json.isEmpty()) return "Không có địa chỉ";
        try {
            // Simple JSON parsing without Jackson dependency in this method
            String detail = extractJsonField(json, "detailAddress");
            String ward = extractJsonField(json, "ward");
            String district = extractJsonField(json, "district");
            String province = extractJsonField(json, "province");
            StringBuilder sb = new StringBuilder();
            if (!detail.isEmpty()) sb.append(detail);
            if (!ward.isEmpty()) sb.append(", ").append(ward);
            if (!district.isEmpty()) sb.append(", ").append(district);
            if (!province.isEmpty()) sb.append(", ").append(province);
            return sb.length() > 0 ? sb.toString() : json;
        } catch (Exception e) {
            return json;
        }
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return "";
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx == -1) return "";
        int start = json.indexOf("\"", colonIdx + 1);
        if (start == -1) return "";
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return MONEY_FMT.format(amount) + " ₫";
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPadding(4);
        lCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lCell);
        PdfPCell vCell = new PdfPCell(new Phrase(value, valueFont));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPadding(4);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vCell);
    }

    private Paragraph createCenteredParagraph(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    // ═══════════════════════════════════════════════════════════
    // REVENUE REPORT EXCEL
    // ═══════════════════════════════════════════════════════════

    @Override
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

            // ═══ Sheet 1: Doanh thu theo thời gian ═══
            Sheet revenueSheet = workbook.createSheet("Doanh thu");
            Row titleRow = revenueSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DOANH THU - " + period);
            titleCell.setCellStyle(titleStyle);

            revenueSheet.createRow(1); // blank row

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
            // Summary row
            Row summaryRow = revenueSheet.createRow(rowIdx + 1);
            summaryRow.createCell(0).setCellValue("TỔNG CỘNG");
            summaryRow.createCell(1).setCellValue(stats.getTotalOrders());
            Cell totalRevCell = summaryRow.createCell(2);
            totalRevCell.setCellValue(stats.getTotalRevenue() != null ? stats.getTotalRevenue().doubleValue() : 0);
            totalRevCell.setCellStyle(currencyStyle);

            for (int i = 0; i < revHeaders.length; i++) revenueSheet.autoSizeColumn(i);

            // ═══ Sheet 2: Top sản phẩm ═══
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

            // ═══ Sheet 3: Top khách hàng ═══
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

            // ═══ Sheet 4: Top danh mục ═══
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
            throw new RuntimeException("Failed to export revenue report", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DASHBOARD REPORT PDF
    // ═══════════════════════════════════════════════════════════

    @Override
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

            // ═══ COMMON HEADER ═══
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

            // Footer
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Hệ thống HoziTech - Báo cáo được tạo tự động", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dashboard report PDF: " + reportType, e);
        }
    }

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

        // Header row: Logo + Date
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

        // Separator
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

        // Title
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

    // ─── ORDERS REPORT ───
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

        // Summary
        document.add(new Paragraph(" "));
        Paragraph summary = new Paragraph("Tổng số đơn hiển thị: " + stats.getRecentOrders().size(), boldFont);
        summary.setAlignment(Element.ALIGN_RIGHT);
        document.add(summary);
    }

    // ─── REVENUE REPORT ───
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

        // Total row
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

    // ─── PRODUCTS REPORT ───
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

    // ─── CUSTOMERS REPORT ───
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

    // ─── RETURNS REPORT ───
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

        // Cancelled
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

        // Returned
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

    // ─── REVIEWS REPORT ───
    private void buildReviewsReportPdf(Document document, DashboardStatsResponse stats,
                                        BaseFont baseFont, Font normalFont, Font boldFont) throws Exception {
        Font bigYellowFont = new Font(baseFont, 48, Font.BOLD, new Color(234, 179, 8));
        Font starFont = new Font(baseFont, 20, Font.NORMAL, new Color(250, 204, 21));
        Font subtitleFont = new Font(baseFont, 10, Font.NORMAL, Color.GRAY);

        document.add(new Paragraph(" "));

        // Average rating
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

        // Rating distribution table
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

    private String mapOrderStatusVi(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "PENDING": return "Chờ xác nhận";
            case "CONFIRMED": return "Đã xác nhận";
            case "PROCESSING": return "Đang xử lý";
            case "SHIPPING": return "Đang giao hàng";
            case "SHIPPED": return "Đã giao hàng";
            case "CANCELLED": return "Đã hủy";
            case "RETURNED": return "Đã hoàn trả";
            default: return status;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EXISTING METHODS (kept as-is)
    // ═══════════════════════════════════════════════════════════

    @Override
    public byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = OrderSpecification.filterForExport(status, keyword, from, to);
        List<Order> orders = orderRepository.findAll(spec);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Đơn hàng");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < ORDER_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ORDER_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getUser() != null ? order.getUser().getFullName() : "");
                row.createCell(2).setCellValue(order.getUser() != null ? order.getUser().getEmail() : "");
                row.createCell(3).setCellValue(order.getUser() != null && order.getUser().getPhoneNumber() != null
                        ? order.getUser().getPhoneNumber() : "");

                Cell subtotalCell = row.createCell(4);
                subtotalCell.setCellValue(order.getSubtotal().doubleValue());
                subtotalCell.setCellStyle(currencyStyle);

                Cell discountCell = row.createCell(5);
                discountCell.setCellValue(order.getDiscountAmount().doubleValue());
                discountCell.setCellStyle(currencyStyle);

                Cell shippingCell = row.createCell(6);
                shippingCell.setCellValue(order.getShippingFee().doubleValue());
                shippingCell.setCellStyle(currencyStyle);

                Cell totalCell = row.createCell(7);
                totalCell.setCellValue(order.getTotalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);

                row.createCell(8).setCellValue(order.getOrderStatus().getDescription());
                row.createCell(9).setCellValue(order.getPaymentMethod().name());
                row.createCell(10).setCellValue(order.getCreatedAt() != null
                        ? order.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < ORDER_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export orders to Excel", e);
        }
    }

    @Override
    public byte[] exportUsersToExcel(String keyword, String role) {
        List<User> users = userRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerKeyword))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerKeyword)))
                    .toList();
        }
        if (role != null && !role.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().getId().name().equalsIgnoreCase(role))
                    .toList();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Người dùng");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < USER_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(USER_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId().toString());
                row.createCell(1).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(3).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                row.createCell(4).setCellValue(user.getRole() != null ? user.getRole().getId().name() : "");
                row.createCell(5).setCellValue(user.getCreatedAt() != null
                        ? user.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < USER_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export users to Excel", e);
        }
    }

    @Override
    public byte[] exportFeedbacksToExcel(String status, UUID productId) {
        List<Feedback> feedbacks = feedbackRepository.findAll();

        if (status != null && !status.isBlank()) {
            feedbacks = feedbacks.stream()
                    .filter(f -> status.equalsIgnoreCase(f.getStatus()))
                    .toList();
        }
        if (productId != null) {
            feedbacks = feedbacks.stream()
                    .filter(f -> f.getProduct() != null && productId.equals(f.getProduct().getId()))
                    .toList();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Đánh giá");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < FEEDBACK_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(FEEDBACK_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Feedback fb : feedbacks) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(fb.getId().toString());
                row.createCell(1).setCellValue(fb.getProduct() != null ? fb.getProduct().getName() : "");
                row.createCell(2).setCellValue(fb.getUser() != null
                        ? (fb.getUser().getFullName() != null ? fb.getUser().getFullName() : fb.getUser().getUserName()) : "");
                row.createCell(3).setCellValue(fb.getUser() != null ? fb.getUser().getEmail() : "");
                row.createCell(4).setCellValue(fb.getRating());
                row.createCell(5).setCellValue(fb.getContent() != null ? fb.getContent() : "");
                row.createCell(6).setCellValue(fb.getStatus() != null ? fb.getStatus() : "");
                row.createCell(7).setCellValue(fb.getAdminReply() != null ? fb.getAdminReply() : "");
                row.createCell(8).setCellValue(fb.getCreatedAt() != null
                        ? fb.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < FEEDBACK_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export feedbacks to Excel", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRODUCTS EXPORT EXCEL
    // ═══════════════════════════════════════════════════════════

    private static final String[] PRODUCT_HEADERS = {
            "STT", "Tên sản phẩm", "SKU/Slug", "Danh mục", "Thương hiệu",
            "Giá gốc", "Tồn kho", "Đã bán", "Trạng thái", "Ngày tạo"
    };

    @Override
    public byte[] exportProductsToExcel(String keyword, UUID categoryId, String status) {
        List<Product> products = productRepository.findAll(
                ProductSpecification.filterForExport(keyword, categoryId, status),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt")));

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

            Sheet sheet = workbook.createSheet("Danh sách sản phẩm");

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DANH SÁCH SẢN PHẨM");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            // Header row
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(PRODUCT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 4;
            long totalSoldSum = 0;
            long totalStockSum = 0;

            for (Product product : products) {
                Row row = sheet.createRow(rowIdx);

                row.createCell(0).setCellValue(rowIdx - 3); // STT
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getSlug());
                row.createCell(3).setCellValue(
                        product.getCategory() != null ? product.getCategory().getName() : "");
                row.createCell(4).setCellValue(
                        product.getBrand() != null ? product.getBrand().getName() : "");

                Cell priceCell = row.createCell(5);
                priceCell.setCellValue(product.getOriginPrice() != null ? product.getOriginPrice().doubleValue() : 0);
                priceCell.setCellStyle(currencyStyle);

                int stock = product.getTotalStock() != null ? product.getTotalStock() : 0;
                row.createCell(6).setCellValue(stock);
                totalStockSum += stock;

                int sold = product.getTotalSold() != null ? product.getTotalSold() : 0;
                row.createCell(7).setCellValue(sold);
                totalSoldSum += sold;

                row.createCell(8).setCellValue(
                        "ACTIVE".equals(product.getStatus()) ? "Đang bán" : "Đã ẩn");
                row.createCell(9).setCellValue(
                        product.getCreatedAt() != null ? product.getCreatedAt().format(DATE_FMT) : "");

                rowIdx++;
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            Cell sumLabel = summaryRow.createCell(0);
            sumLabel.setCellValue("TỔNG CỘNG: " + products.size() + " sản phẩm");
            CellStyle boldStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFontPoi = workbook.createFont();
            boldFontPoi.setBold(true);
            boldStyle.setFont(boldFontPoi);
            sumLabel.setCellStyle(boldStyle);

            Cell sumStock = summaryRow.createCell(6);
            sumStock.setCellValue(totalStockSum);
            sumStock.setCellStyle(boldStyle);

            Cell sumSold = summaryRow.createCell(7);
            sumSold.setCellValue(totalSoldSum);
            sumSold.setCellStyle(boldStyle);

            // Auto-size columns
            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export products to Excel", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        return headerStyle;
    }


}
