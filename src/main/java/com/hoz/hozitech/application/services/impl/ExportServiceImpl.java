package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.services.ExportService;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;

    private static final String[] HEADERS = {
            "Mã đơn", "Khách hàng", "Email", "SĐT",
            "Tạm tính", "Giảm giá", "Phí ship", "Thành tiền",
            "Trạng thái", "Thanh toán", "Ngày đặt"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = buildSpec(status, keyword, from, to);
        List<Order> orders = orderRepository.findAll(spec);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Đơn hàng");

            // --- Header Style ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // --- Currency Style ---
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            // --- Header Row ---
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
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

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export orders to Excel", e);
        }
    }

    private Specification<Order> buildSpec(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = Specification.where((Specification<Order>) null);

        // Fetch user eagerly to avoid N+1
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
