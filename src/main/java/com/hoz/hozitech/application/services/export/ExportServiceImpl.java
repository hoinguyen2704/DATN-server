package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.dashboard.DashboardService;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.domain.enums.OrderStatus;
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
            String payStatus = order.getPaymentStatus().name().equals("PAID") ? "Đã thanh toán" : "Chưa thu tiền";
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
    // EXISTING METHODS (kept as-is)
    // ═══════════════════════════════════════════════════════════

    @Override
    public byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = buildSpec(status, keyword, from, to);
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

    private Specification<Order> buildSpec(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = Specification.where((root, query, cb) -> cb.conjunction());

        spec = spec.and((root, query, cb) -> {
            root.fetch("user", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.conjunction();
        });

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("orderStatus"), OrderStatus.valueOf(status.toUpperCase())));
        }

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                String pattern = "%" + keyword.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(root.join("user").get("fullName")), pattern),
                        cb.like(cb.lower(root.join("user").get("email")), pattern)
                );
            });
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return spec;
    }
}
