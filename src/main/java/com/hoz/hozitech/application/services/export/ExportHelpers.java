package com.hoz.hozitech.application.services.export;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.hoz.hozitech.web.exceptions.ExportException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Shared helpers for PDF and Excel export operations.
 * Extracted from ExportServiceImpl to eliminate duplication across sub-exporters.
 */
public final class ExportHelpers {

    private ExportHelpers() {}

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    public static final Color PRIMARY_COLOR = new Color(37, 57, 230);

    // ─── PDF Helpers ───

    public static BaseFont loadVietnameseFont() {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/OpenSans.ttf");
            String fontPath = fontResource.getFile().getAbsolutePath();
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                throw new ExportException("Cannot load font", ex);
            }
        }
    }

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return MONEY_FMT.format(amount) + " ₫";
    }

    public static void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
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

    public static Paragraph createCenteredParagraph(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    public static String parseAddress(String json) {
        if (json == null || json.isEmpty()) return "Không có địa chỉ";
        try {
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

    public static String extractJsonField(String json, String field) {
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

    public static String mapOrderStatusVi(String status) {
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

    // ─── Excel Helpers ───

    public static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
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
