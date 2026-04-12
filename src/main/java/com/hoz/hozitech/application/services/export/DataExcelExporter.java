package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.ReturnRequestRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.application.specifications.ProductSpecification;
import com.hoz.hozitech.application.specifications.ReturnRequestSpecification;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.Order;

import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import com.hoz.hozitech.web.exceptions.ExportException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.hoz.hozitech.application.services.export.ExportHelpers.*;

/**
 * Excel data exports for Orders, Users, Feedbacks, Products, and Return Requests.
 */
@Component
@RequiredArgsConstructor
public class DataExcelExporter {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final ReturnRequestRepository returnRequestRepository;

    private static final String[] ORDER_HEADERS = {
            "Mã đơn", "Khách hàng", "Email", "SĐT",
            "Tạm tính", "Giảm giá", "Phí ship", "Thuế", "Thành tiền",
            "Trạng thái", "Thanh toán", "Ngày đặt"
    };

    private static final String[] USER_HEADERS = {
            "ID", "Họ tên", "Email", "SĐT", "Vai trò", "Ngày tạo"
    };

    private static final String[] FEEDBACK_HEADERS = {
            "ID", "Sản phẩm", "Khách hàng", "Email", "Đánh giá", "Nội dung",
            "Trạng thái", "Phản hồi admin", "Ngày tạo"
    };

    private static final String[] PRODUCT_HEADERS = {
            "STT", "Tên sản phẩm", "SKU/Slug", "Danh mục", "Thương hiệu",
            "Giá gốc", "Tồn kho", "Đã bán", "Trạng thái", "Ngày tạo"
    };

    private static final String[] RETURN_HEADERS = {
            "STT", "Mã yêu cầu", "Mã đơn hàng", "Khách hàng", "Email",
            "Lý do", "Số tiền yêu cầu", "Số tiền duyệt", "Số tiền hoàn",
            "Trạng thái", "Hoàn tiền", "Ngày tạo", "Ngày xử lý"
    };

    // Orders Export

    public byte[] exportOrdersToExcel(String status, String keyword, LocalDateTime from, LocalDateTime to) {
        Specification<Order> spec = OrderSpecification.filterForExport(status, keyword, from, to);
        List<Order> orders = orderRepository.findAll(spec);

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

            Sheet sheet = workbook.createSheet("Đơn hàng");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH ĐƠN HÀNG");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < ORDER_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(ORDER_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getUser() != null ? order.getUser().getFullName() : "");
                row.createCell(2).setCellValue(order.getUser() != null ? order.getUser().getEmail() : "");
                row.createCell(3)
                        .setCellValue(order.getUser() != null && order.getUser().getPhoneNumber() != null
                                ? order.getUser().getPhoneNumber()
                                : "");

                Cell subtotalCell = row.createCell(4);
                subtotalCell.setCellValue(order.getSubtotal() != null ? order.getSubtotal().doubleValue() : 0);
                subtotalCell.setCellStyle(currencyStyle);

                Cell discountCell = row.createCell(5);
                discountCell
                        .setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0);
                discountCell.setCellStyle(currencyStyle);

                Cell shippingCell = row.createCell(6);
                shippingCell.setCellValue(order.getShippingFee() != null ? order.getShippingFee().doubleValue() : 0);
                shippingCell.setCellStyle(currencyStyle);

                Cell taxCell = row.createCell(7);
                taxCell.setCellValue(order.getTaxAmount() != null ? order.getTaxAmount().doubleValue() : 0);
                taxCell.setCellStyle(currencyStyle);

                Cell totalCell = row.createCell(8);
                totalCell.setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
                totalCell.setCellStyle(currencyStyle);

                row.createCell(9).setCellValue(mapOrderStatusVi(
                        order.getOrderStatus() != null ? order.getOrderStatus().name() : ""));
                row.createCell(10).setCellValue(
                        order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "");
                row.createCell(11).setCellValue(
                        order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < ORDER_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export orders to Excel", e);
        }
    }

    // Users Export

    public byte[] exportUsersToExcel(String keyword, String role) {
        List<User> users = userRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFontPoi = workbook.createFont();
            titleFontPoi.setBold(true);
            titleFontPoi.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFontPoi);

            Sheet sheet = workbook.createSheet("Người dùng");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH NGƯỜI DÙNG");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < USER_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(USER_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (User user : users) {
                if (keyword != null && !keyword.isEmpty()) {
                    String kw = keyword.toLowerCase();
                    boolean match = (user.getFullName() != null && user.getFullName().toLowerCase().contains(kw))
                            || (user.getEmail() != null && user.getEmail().toLowerCase().contains(kw));
                    if (!match)
                        continue;
                }
                if (role != null && !role.isEmpty()) {
                    boolean hasRole = user.getRole() != null
                            && user.getRole().getId().name().equalsIgnoreCase(role);
                    if (!hasRole)
                        continue;
                }

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId().toString());
                row.createCell(1).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(3).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                row.createCell(4).setCellValue(
                        user.getRole() != null ? user.getRole().getId().name() : "");
                row.createCell(5).setCellValue(
                        user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < USER_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export users to Excel", e);
        }
    }

    // Feedbacks Export

    public byte[] exportFeedbacksToExcel(String status, UUID productId) {
        List<Feedback> feedbacks = feedbackRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFontPoi = workbook.createFont();
            titleFontPoi.setBold(true);
            titleFontPoi.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFontPoi);

            Sheet sheet = workbook.createSheet("Đánh giá");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH ĐÁNH GIÁ");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < FEEDBACK_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(FEEDBACK_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (Feedback fb : feedbacks) {
                if (status != null && !status.isEmpty() && !status.equalsIgnoreCase(fb.getStatus().name()))
                    continue;
                if (productId != null && !productId.equals(fb.getProduct().getId()))
                    continue;

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(fb.getId().toString());
                row.createCell(1).setCellValue(fb.getProduct() != null ? fb.getProduct().getName() : "");
                row.createCell(2).setCellValue(fb.getUser() != null ? fb.getUser().getFullName() : "");
                row.createCell(3).setCellValue(fb.getUser() != null ? fb.getUser().getEmail() : "");
                row.createCell(4).setCellValue(fb.getRating());
                row.createCell(5).setCellValue(fb.getContent() != null ? fb.getContent() : "");
                row.createCell(6).setCellValue(fb.getStatus() != null ? fb.getStatus().name() : "");
                row.createCell(7).setCellValue(fb.getAdminReply() != null ? fb.getAdminReply() : "");
                row.createCell(8).setCellValue(
                        fb.getCreatedAt() != null ? fb.getCreatedAt().format(DATE_FMT) : "");
            }

            for (int i = 0; i < FEEDBACK_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export feedbacks to Excel", e);
        }
    }

    // Products Export

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

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DANH SÁCH SẢN PHẨM");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(PRODUCT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            long totalSoldSum = 0;
            long totalStockSum = 0;

            for (Product product : products) {
                Row row = sheet.createRow(rowIdx);

                row.createCell(0).setCellValue(rowIdx - 3);
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
                        com.hoz.hozitech.domain.enums.ProductStatus.ACTIVE == product.getStatus() ? "Đang bán"
                                : "Đã ẩn");
                row.createCell(9).setCellValue(
                        product.getCreatedAt() != null ? product.getCreatedAt().format(DATE_FMT) : "");

                rowIdx++;
            }

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

            for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export products to Excel", e);
        }
    }

    // Returns Export

    public byte[] exportReturnsToExcel(String status, String keyword) {
        ReturnRequestStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ReturnRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // invalid status string → skip filter
            }
        }

        Specification<ReturnRequest> spec = ReturnRequestSpecification.filter(null, statusEnum, keyword);
        List<ReturnRequest> returns = returnRequestRepository.findAll(spec,
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

            Sheet sheet = workbook.createSheet("Đơn hoàn hủy");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO ĐƠN HOÀN HỦY");
            titleCell.setCellStyle(titleStyle);

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < RETURN_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(RETURN_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            double totalRequested = 0;
            double totalApproved = 0;
            double totalRefunded = 0;

            for (ReturnRequest rr : returns) {
                Row row = sheet.createRow(rowIdx);

                row.createCell(0).setCellValue(rowIdx - 3);
                row.createCell(1).setCellValue(rr.getReturnNumber());
                row.createCell(2).setCellValue(
                        rr.getOrder() != null ? rr.getOrder().getOrderNumber() : "");
                row.createCell(3).setCellValue(
                        rr.getUser() != null ? rr.getUser().getFullName() : "");
                row.createCell(4).setCellValue(
                        rr.getUser() != null ? rr.getUser().getEmail() : "");
                row.createCell(5).setCellValue(
                        rr.getReason() != null ? rr.getReason() : "");

                double requested = rr.getRequestedAmount() != null ? rr.getRequestedAmount().doubleValue() : 0;
                Cell reqCell = row.createCell(6);
                reqCell.setCellValue(requested);
                reqCell.setCellStyle(currencyStyle);
                totalRequested += requested;

                double approved = rr.getApprovedAmount() != null ? rr.getApprovedAmount().doubleValue() : 0;
                Cell approvedCell = row.createCell(7);
                approvedCell.setCellValue(approved);
                approvedCell.setCellStyle(currencyStyle);
                totalApproved += approved;

                double refunded = rr.getRefundAmount() != null ? rr.getRefundAmount().doubleValue() : 0;
                Cell refundCell = row.createCell(8);
                refundCell.setCellValue(refunded);
                refundCell.setCellStyle(currencyStyle);
                totalRefunded += refunded;

                row.createCell(9).setCellValue(mapReturnStatusVi(
                        rr.getStatus() != null ? rr.getStatus().name() : ""));
                row.createCell(10).setCellValue(mapRefundStatusVi(
                        rr.getRefundStatus() != null ? rr.getRefundStatus().name() : ""));
                row.createCell(11).setCellValue(
                        rr.getCreatedAt() != null ? rr.getCreatedAt().format(DATE_FMT) : "");
                row.createCell(12).setCellValue(
                        rr.getResolvedAt() != null ? rr.getResolvedAt().format(DATE_FMT) : "");

                rowIdx++;
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            CellStyle boldStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFontPoi = workbook.createFont();
            boldFontPoi.setBold(true);
            boldStyle.setFont(boldFontPoi);

            Cell sumLabel = summaryRow.createCell(0);
            sumLabel.setCellValue("TỔNG CỘNG: " + returns.size() + " yêu cầu");
            sumLabel.setCellStyle(boldStyle);

            CellStyle boldCurrency = workbook.createCellStyle();
            boldCurrency.cloneStyleFrom(currencyStyle);
            boldCurrency.setFont(boldFontPoi);

            Cell sumReq = summaryRow.createCell(6);
            sumReq.setCellValue(totalRequested);
            sumReq.setCellStyle(boldCurrency);

            Cell sumApp = summaryRow.createCell(7);
            sumApp.setCellValue(totalApproved);
            sumApp.setCellStyle(boldCurrency);

            Cell sumRef = summaryRow.createCell(8);
            sumRef.setCellValue(totalRefunded);
            sumRef.setCellStyle(boldCurrency);

            for (int i = 0; i < RETURN_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new ExportException("Failed to export returns to Excel", e);
        }
    }

    private static String mapReturnStatusVi(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "REQUESTED" -> "Yêu cầu trả hàng";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Đã từ chối";
            case "IN_TRANSIT" -> "Đang gửi hàng hoàn";
            case "RECEIVED" -> "Đã nhận hàng hoàn";
            case "QC_PASSED" -> "QC đạt";
            case "QC_FAILED" -> "QC không đạt";
            case "REFUND_PENDING" -> "Chờ hoàn tiền";
            case "REFUNDED" -> "Đã hoàn tiền";
            case "CANCELLED" -> "Đã hủy yêu cầu";
            case "CLOSED" -> "Đã đóng";
            default -> status;
        };
    }

    private static String mapRefundStatusVi(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ hoàn tiền";
            case "PROCESSING" -> "Đang xử lý";
            case "SUCCESS" -> "Hoàn tiền thành công";
            case "FAILED" -> "Hoàn tiền thất bại";
            case "REVERSED" -> "Hoàn tiền bị đảo ngược";
            default -> status;
        };
    }
}
