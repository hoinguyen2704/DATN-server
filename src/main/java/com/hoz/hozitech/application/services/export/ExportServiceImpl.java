package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.export.ExportService;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.User;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;

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

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        return headerStyle;
    }

    private Specification<Order> buildSpec(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = Specification.where((Specification<Order>) null);

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
