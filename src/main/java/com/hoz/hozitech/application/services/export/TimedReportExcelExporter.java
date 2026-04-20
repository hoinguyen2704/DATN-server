package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.OrderItemRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.repositories.ReturnRequestRepository;
import com.hoz.hozitech.application.specifications.OrderSpecification;
import com.hoz.hozitech.application.specifications.ReturnRequestSpecification;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.RevenueChartItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopCategoryItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopCustomerItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopProductItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopVariantItem;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.domain.enums.RefundStatus;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import com.hoz.hozitech.web.exceptions.ExportException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.hoz.hozitech.application.services.export.ExportHelpers.DATE_FMT;
import static com.hoz.hozitech.application.services.export.ExportHelpers.createHeaderStyle;
import static com.hoz.hozitech.application.services.export.ExportHelpers.mapOrderStatusVi;

@Component
@RequiredArgsConstructor
public class TimedReportExcelExporter {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final CouponRepository couponRepository;

    public byte[] exportRevenueReport(ReportDateRange range) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);
            RevenueReportData data = buildRevenueReportData(range);

            writeRevenueSheet(workbook, styles, range, data);
            writeTopProductsSheet(workbook, styles, range, data.topProducts());
            writeTopVariantsSheet(workbook, styles, range, data.topVariants());
            writeTopCustomersSheet(workbook, styles, range, data.topCustomers());
            writeTopCategoriesSheet(workbook, styles, range, data.topCategories());

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Failed to export revenue report by range", ex);
        }
    }

    public byte[] exportOrdersReport(String status, String keyword, ReportDateRange range) {
        Specification<Order> spec = OrderSpecification.filterForExport(status, keyword, range.from(), range.to());
        List<Order> orders = new ArrayList<>(orderRepository.findAll(spec));
        orders.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);
            writeOrdersSummarySheet(workbook, styles, range, orders);
            writeOrdersDetailSheet(workbook, styles, range, orders);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Failed to export orders report by range", ex);
        }
    }

    public byte[] exportReturnsReport(String status, String keyword, ReportDateRange range) {
        Specification<ReturnRequest> spec = ReturnRequestSpecification.filterForExport(status, keyword, range.from(), range.to());
        List<ReturnRequest> returns = new ArrayList<>(returnRequestRepository.findAll(spec));
        returns.sort(Comparator.comparing(ReturnRequest::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);
            writeReturnsSummarySheet(workbook, styles, range, returns);
            writeReturnsDetailSheet(workbook, styles, range, returns);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Failed to export returns report by range", ex);
        }
    }

    public byte[] exportVouchersReport(String keyword, ReportDateRange range) {
        Specification<Order> spec = OrderSpecification.filterForExport(null, null, range.from(), range.to());
        List<Order> orders = new ArrayList<>(orderRepository.findAll(spec));
        orders.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

        VoucherReportData data = buildVoucherReportData(orders, keyword);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);
            writeVoucherSummarySheet(workbook, styles, range, data);
            writeVoucherDetailSheet(workbook, styles, range, data.details());

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Failed to export vouchers report by range", ex);
        }
    }

    private RevenueReportData buildRevenueReportData(ReportDateRange range) {
        List<RevenueChartItem> revenueChart = buildRevenueChart(range);
        BigDecimal totalRevenue = revenueChart.stream()
                .map(RevenueChartItem::getRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = revenueChart.stream().mapToLong(RevenueChartItem::getOrders).sum();

        List<TopProductItem> topProducts = buildTopProducts(range.from(), range.to());
        List<TopVariantItem> topVariants = buildTopVariants(range.from(), range.to(), 10);
        List<TopCustomerItem> topCustomers = buildTopCustomers(range.from(), range.to());
        List<TopCategoryItem> topCategories = buildTopCategories(range.from(), range.to());

        return new RevenueReportData(totalRevenue, totalOrders, revenueChart, topProducts, topVariants, topCustomers, topCategories);
    }

    private List<RevenueChartItem> buildRevenueChart(ReportDateRange range) {
        if (range.mode() == ReportRangeMode.YEAR) {
            int year = range.from().getYear();
            return orderRepository.findRevenueGroupedByMonth(year).stream()
                    .map(row -> RevenueChartItem.builder()
                            .label("Tháng " + ((Number) row[0]).intValue())
                            .revenue((BigDecimal) row[1])
                            .orders(((Number) row[2]).longValue())
                            .build())
                    .toList();
        }

        return orderRepository.findRevenueGroupedByDate(range.from(), range.to()).stream()
                .map(row -> RevenueChartItem.builder()
                        .label(row[0].toString())
                        .revenue((BigDecimal) row[1])
                        .orders(((Number) row[2]).longValue())
                        .build())
                .toList();
    }

    private List<TopProductItem> buildTopProducts(LocalDateTime from, LocalDateTime to) {
        return orderItemRepository.findTopSellingProducts(from, to, PageRequest.of(0, 10)).stream()
                .map(row -> TopProductItem.builder()
                        .id(((UUID) row[0]).toString())
                        .name((String) row[1])
                        .totalSold(((Number) row[2]).longValue())
                        .revenue((BigDecimal) row[3])
                        .build())
                .toList();
    }

    private List<TopVariantItem> buildTopVariants(LocalDateTime from, LocalDateTime to, int limit) {
        List<Object[]> rows = orderItemRepository.findTopSellingVariants(from, to, PageRequest.of(0, limit));
        List<UUID> variantIds = rows.stream()
                .map(row -> (UUID) row[0])
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Long> returnedByVariantId = variantIds.isEmpty()
                ? Map.of()
                : returnItemRepository.sumReturnedQuantityByVariantIdsBetween(variantIds, from, to).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));

        return rows.stream().map(row -> {
            UUID variantId = (UUID) row[0];
            long grossSoldQty = ((Number) row[4]).longValue();
            long returnedQty = returnedByVariantId.getOrDefault(variantId, 0L);
            return TopVariantItem.builder()
                    .variantId(variantId.toString())
                    .productId(row[1].toString())
                    .productName((String) row[2])
                    .variantName((String) row[3])
                    .totalSold(grossSoldQty)
                    .grossSoldQty(grossSoldQty)
                    .returnedQty(returnedQty)
                    .netSoldQty(Math.max(grossSoldQty - returnedQty, 0L))
                    .revenue((BigDecimal) row[5])
                    .build();
        }).toList();
    }

    private List<TopCustomerItem> buildTopCustomers(LocalDateTime from, LocalDateTime to) {
        return orderRepository.findTopCustomers(from, to, PageRequest.of(0, 10)).stream()
                .map(row -> TopCustomerItem.builder()
                        .id(row[0].toString())
                        .name((String) row[1])
                        .email((String) row[2])
                        .totalOrders(((Number) row[3]).longValue())
                        .totalSpent((BigDecimal) row[4])
                        .build())
                .toList();
    }

    private List<TopCategoryItem> buildTopCategories(LocalDateTime from, LocalDateTime to) {
        return orderItemRepository.findTopSellingCategories(from, to, PageRequest.of(0, 10)).stream()
                .map(row -> TopCategoryItem.builder()
                        .id(row[0].toString())
                        .name((String) row[1])
                        .totalSold(((Number) row[2]).longValue())
                        .revenue((BigDecimal) row[3])
                        .build())
                .toList();
    }

    private VoucherReportData buildVoucherReportData(List<Order> orders, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Map<String, VoucherAccumulator> usageByCode = new LinkedHashMap<>();
        long ordersUsingVoucher = 0L;
        BigDecimal totalProductDiscount = BigDecimal.ZERO;
        BigDecimal totalShippingDiscount = BigDecimal.ZERO;

        for (Order order : orders) {
            if (!isValidVoucherOrder(order)) {
                continue;
            }

            boolean matchedAnyVoucher = false;

            if (isMatchingVoucherCode(order.getCouponCode(), normalizedKeyword)
                    && isPositive(order.getDiscountAmount())) {
                matchedAnyVoucher = true;
                totalProductDiscount = totalProductDiscount.add(nullSafe(order.getDiscountAmount()));
                usageByCode.computeIfAbsent(order.getCouponCode().toUpperCase(Locale.ROOT), VoucherAccumulator::new)
                        .recordUsage(CouponCategory.PRODUCT, order.getDiscountAmount());
            }

            if (isMatchingVoucherCode(order.getShippingCouponCode(), normalizedKeyword)
                    && isPositive(order.getShippingDiscountAmount())) {
                matchedAnyVoucher = true;
                totalShippingDiscount = totalShippingDiscount.add(nullSafe(order.getShippingDiscountAmount()));
                usageByCode.computeIfAbsent(order.getShippingCouponCode().toUpperCase(Locale.ROOT), VoucherAccumulator::new)
                        .recordUsage(CouponCategory.SHIPPING, order.getShippingDiscountAmount());
            }

            if (matchedAnyVoucher) {
                ordersUsingVoucher++;
            }
        }

        Collection<String> codes = usageByCode.keySet();
        Map<String, Coupon> couponByCode = codes.isEmpty()
                ? Map.of()
                : couponRepository.findAllByUpperCodeIn(codes).stream()
                .collect(Collectors.toMap(
                        coupon -> coupon.getCode().toUpperCase(Locale.ROOT),
                        coupon -> coupon,
                        (left, right) -> left));

        List<VoucherUsageDetail> details = usageByCode.values().stream()
                .map(accumulator -> accumulator.toDetail(couponByCode.get(accumulator.code())))
                .filter(detail -> detail.orderCount() > 0)
                .sorted(Comparator.comparing(VoucherUsageDetail::totalDiscount).reversed())
                .toList();

        BigDecimal totalDiscount = totalProductDiscount.add(totalShippingDiscount);
        return new VoucherReportData(
                details.size(),
                ordersUsingVoucher,
                totalProductDiscount,
                totalShippingDiscount,
                totalDiscount,
                details);
    }

    private boolean isValidVoucherOrder(Order order) {
        return order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getPaymentStatus() != PaymentStatus.FAILED;
    }

    private boolean isMatchingVoucherCode(String code, String normalizedKeyword) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return normalizedKeyword.isBlank()
                || code.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);

        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle subtitleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
        subtitleFont.setItalic(true);
        subtitleStyle.setFont(subtitleFont);

        CellStyle boldStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle boldCurrencyStyle = workbook.createCellStyle();
        boldCurrencyStyle.cloneStyleFrom(currencyStyle);
        boldCurrencyStyle.setFont(boldFont);

        return new Styles(headerStyle, titleStyle, subtitleStyle, boldStyle, currencyStyle, boldCurrencyStyle);
    }

    private void writeRevenueSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, RevenueReportData data) {
        Sheet sheet = workbook.createSheet("Doanh thu");
        int rowIndex = writeSheetHeader(sheet, styles, "BÁO CÁO DOANH THU", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex, "Thời gian", "Số đơn hàng", "Doanh thu (VNĐ)");

        for (RevenueChartItem item : data.revenueChart()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(item.getLabel()));
            row.createCell(1).setCellValue(item.getOrders());
            applyCurrency(row.createCell(2), item.getRevenue(), styles.currencyStyle());
        }

        Row summaryRow = sheet.createRow(rowIndex + 1);
        Cell labelCell = summaryRow.createCell(0);
        labelCell.setCellValue("TỔNG CỘNG");
        labelCell.setCellStyle(styles.boldStyle());
        summaryRow.createCell(1).setCellValue(data.totalOrders());
        applyCurrency(summaryRow.createCell(2), data.totalRevenue(), styles.boldCurrencyStyle());

        autoSize(sheet, 3);
    }

    private void writeTopProductsSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<TopProductItem> topProducts) {
        Sheet sheet = workbook.createSheet("Top sản phẩm");
        int rowIndex = writeSheetHeader(sheet, styles, "TOP SẢN PHẨM BÁN CHẠY", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex, "#", "Sản phẩm", "Đã bán", "Doanh thu (VNĐ)");

        int rank = 1;
        for (TopProductItem item : topProducts) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(item.getTotalSold());
            applyCurrency(row.createCell(3), item.getRevenue(), styles.currencyStyle());
        }

        autoSize(sheet, 4);
    }

    private void writeTopVariantsSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<TopVariantItem> topVariants) {
        Sheet sheet = workbook.createSheet("Top phân loại");
        int rowIndex = writeSheetHeader(sheet, styles, "TOP PHÂN LOẠI BÁN CHẠY", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "#", "Sản phẩm", "Phân loại", "Gross", "Return", "Net", "Doanh thu (VNĐ)");

        int rank = 1;
        for (TopVariantItem item : topVariants) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(safe(item.getProductName()));
            row.createCell(2).setCellValue(safe(item.getVariantName(), "Mặc định"));
            row.createCell(3).setCellValue(item.getGrossSoldQty());
            row.createCell(4).setCellValue(item.getReturnedQty());
            row.createCell(5).setCellValue(item.getNetSoldQty());
            applyCurrency(row.createCell(6), item.getRevenue(), styles.currencyStyle());
        }

        autoSize(sheet, 7);
    }

    private void writeTopCustomersSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<TopCustomerItem> topCustomers) {
        Sheet sheet = workbook.createSheet("Top khách hàng");
        int rowIndex = writeSheetHeader(sheet, styles, "TOP KHÁCH HÀNG TIỀM NĂNG", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "#", "Khách hàng", "Email", "Số đơn", "Tổng chi tiêu (VNĐ)");

        int rank = 1;
        for (TopCustomerItem item : topCustomers) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(safe(item.getEmail()));
            row.createCell(3).setCellValue(item.getTotalOrders());
            applyCurrency(row.createCell(4), item.getTotalSpent(), styles.currencyStyle());
        }

        autoSize(sheet, 5);
    }

    private void writeTopCategoriesSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<TopCategoryItem> topCategories) {
        Sheet sheet = workbook.createSheet("Danh mục");
        int rowIndex = writeSheetHeader(sheet, styles, "DOANH THU THEO DANH MỤC", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "#", "Danh mục", "Đã bán", "Doanh thu (VNĐ)");

        int rank = 1;
        for (TopCategoryItem item : topCategories) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(rank++);
            row.createCell(1).setCellValue(safe(item.getName()));
            row.createCell(2).setCellValue(item.getTotalSold());
            applyCurrency(row.createCell(3), item.getRevenue(), styles.currencyStyle());
        }

        autoSize(sheet, 4);
    }

    private void writeOrdersSummarySheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<Order> orders) {
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = orders.stream()
                .map(order -> nullSafe(order.getDiscountAmount()).add(nullSafe(order.getShippingDiscountAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cancelledCount = orders.stream().filter(order -> order.getOrderStatus() == OrderStatus.CANCELLED).count();
        long returnedCount = orders.stream().filter(order -> order.getOrderStatus() == OrderStatus.RETURNED).count();

        Sheet sheet = workbook.createSheet("Tổng quan");
        int rowIndex = writeSheetHeader(sheet, styles, "BÁO CÁO ĐƠN HÀNG", range);
        rowIndex = writeKeyValueHeader(sheet, styles.headerStyle(), rowIndex, "Chỉ số", "Giá trị");

        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng số đơn", orders.size(), false);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng doanh thu", totalRevenue, true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng giảm giá", totalDiscount, true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số đơn hủy", cancelledCount, false);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số đơn hoàn", returnedCount, false);

        autoSize(sheet, 2);
    }

    private void writeOrdersDetailSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<Order> orders) {
        Sheet sheet = workbook.createSheet("Chi tiết đơn hàng");
        int rowIndex = writeSheetHeader(sheet, styles, "CHI TIẾT ĐƠN HÀNG", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "Mã đơn", "Khách hàng", "Email", "SĐT",
                "Tạm tính", "Giảm giá SP", "Giảm ship", "Phí ship", "Thuế", "Thành tiền",
                "Trạng thái", "Thanh toán", "Ngày đặt");

        for (Order order : orders) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(order.getOrderNumber()));
            row.createCell(1).setCellValue(order.getUser() != null ? safe(order.getUser().getFullName()) : "");
            row.createCell(2).setCellValue(order.getUser() != null ? safe(order.getUser().getEmail()) : "");
            row.createCell(3).setCellValue(order.getUser() != null ? safe(order.getUser().getPhoneNumber()) : "");
            applyCurrency(row.createCell(4), order.getSubtotal(), styles.currencyStyle());
            applyCurrency(row.createCell(5), order.getDiscountAmount(), styles.currencyStyle());
            applyCurrency(row.createCell(6), order.getShippingDiscountAmount(), styles.currencyStyle());
            applyCurrency(row.createCell(7), order.getShippingFee(), styles.currencyStyle());
            applyCurrency(row.createCell(8), order.getTaxAmount(), styles.currencyStyle());
            applyCurrency(row.createCell(9), order.getTotalAmount(), styles.currencyStyle());
            row.createCell(10).setCellValue(mapOrderStatusVi(order.getOrderStatus() != null ? order.getOrderStatus().name() : ""));
            row.createCell(11).setCellValue(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "");
            row.createCell(12).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "");
        }

        autoSize(sheet, 13);
    }

    private void writeReturnsSummarySheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<ReturnRequest> returns) {
        BigDecimal totalRequested = returns.stream().map(ReturnRequest::getRequestedAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalApproved = returns.stream().map(ReturnRequest::getApprovedAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefunded = returns.stream().map(ReturnRequest::getRefundAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingCount = returns.stream()
                .filter(item -> item.getStatus() == ReturnRequestStatus.REQUESTED)
                .count();

        Sheet sheet = workbook.createSheet("Tổng quan");
        int rowIndex = writeSheetHeader(sheet, styles, "BÁO CÁO HOÀN TRẢ", range);
        rowIndex = writeKeyValueHeader(sheet, styles.headerStyle(), rowIndex, "Chỉ số", "Giá trị");

        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số yêu cầu hoàn", returns.size(), false);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng tiền yêu cầu", totalRequested, true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng tiền duyệt", totalApproved, true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng tiền hoàn", totalRefunded, true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số yêu cầu chờ xử lý", pendingCount, false);

        autoSize(sheet, 2);
    }

    private void writeReturnsDetailSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<ReturnRequest> returns) {
        Sheet sheet = workbook.createSheet("Chi tiết hoàn trả");
        int rowIndex = writeSheetHeader(sheet, styles, "CHI TIẾT HOÀN TRẢ", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "Mã yêu cầu", "Mã đơn", "Khách hàng", "Email",
                "Trạng thái", "Hoàn tiền", "Tiền yêu cầu", "Tiền duyệt", "Tiền hoàn",
                "Ngày tạo", "Ngày xử lý");

        for (ReturnRequest item : returns) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(item.getReturnNumber()));
            row.createCell(1).setCellValue(item.getOrder() != null ? safe(item.getOrder().getOrderNumber()) : "");
            row.createCell(2).setCellValue(item.getUser() != null ? safe(item.getUser().getFullName()) : "");
            row.createCell(3).setCellValue(item.getUser() != null ? safe(item.getUser().getEmail()) : "");
            row.createCell(4).setCellValue(mapReturnStatusVi(item.getStatus()));
            row.createCell(5).setCellValue(mapRefundStatusVi(item.getRefundStatus()));
            applyCurrency(row.createCell(6), item.getRequestedAmount(), styles.currencyStyle());
            applyCurrency(row.createCell(7), item.getApprovedAmount(), styles.currencyStyle());
            applyCurrency(row.createCell(8), item.getRefundAmount(), styles.currencyStyle());
            row.createCell(9).setCellValue(item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FMT) : "");
            row.createCell(10).setCellValue(item.getResolvedAt() != null ? item.getResolvedAt().format(DATE_FMT) : "");
        }

        autoSize(sheet, 11);
    }

    private void writeVoucherSummarySheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, VoucherReportData data) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        int rowIndex = writeSheetHeader(sheet, styles, "BÁO CÁO HIỆU QUẢ VOUCHER", range);
        rowIndex = writeKeyValueHeader(sheet, styles.headerStyle(), rowIndex, "Chỉ số", "Giá trị");

        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số voucher được dùng", data.voucherCount(), false);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Số đơn áp dụng voucher", data.orderCount(), false);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng giảm giá sản phẩm", data.totalProductDiscount(), true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng giảm giá ship", data.totalShippingDiscount(), true);
        rowIndex = writeSummaryRow(sheet, rowIndex, styles, "Tổng giảm giá", data.totalDiscount(), true);

        autoSize(sheet, 2);
    }

    private void writeVoucherDetailSheet(XSSFWorkbook workbook, Styles styles, ReportDateRange range, List<VoucherUsageDetail> details) {
        Sheet sheet = workbook.createSheet("Hiệu quả voucher");
        int rowIndex = writeSheetHeader(sheet, styles, "CHI TIẾT HIỆU QUẢ VOUCHER", range);
        rowIndex = writeTableHeader(sheet, styles.headerStyle(), rowIndex,
                "Mã voucher", "Nhóm", "Số đơn áp dụng", "Tổng discount", "TB discount / đơn",
                "Used count", "Usage limit", "Trạng thái", "Bắt đầu", "Kết thúc");

        for (VoucherUsageDetail detail : details) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(detail.code());
            row.createCell(1).setCellValue(detail.categoryLabel());
            row.createCell(2).setCellValue(detail.orderCount());
            applyCurrency(row.createCell(3), detail.totalDiscount(), styles.currencyStyle());
            applyCurrency(row.createCell(4), detail.averageDiscount(), styles.currencyStyle());
            row.createCell(5).setCellValue(detail.usedCount());
            row.createCell(6).setCellValue(detail.usageLimitLabel());
            row.createCell(7).setCellValue(detail.statusLabel());
            row.createCell(8).setCellValue(detail.startDateLabel());
            row.createCell(9).setCellValue(detail.endDateLabel());
        }

        autoSize(sheet, 10);
    }

    private int writeSheetHeader(Sheet sheet, Styles styles, String title, ReportDateRange range) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.titleStyle());

        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Khoảng thời gian: " + range.displayLabel());
        dateCell.setCellStyle(styles.subtitleStyle());

        Row exportRow = sheet.createRow(2);
        exportRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

        return 4;
    }

    private int writeTableHeader(Sheet sheet, CellStyle headerStyle, int rowIndex, String... headers) {
        Row headerRow = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return rowIndex + 1;
    }

    private int writeKeyValueHeader(Sheet sheet, CellStyle headerStyle, int rowIndex, String left, String right) {
        return writeTableHeader(sheet, headerStyle, rowIndex, left, right);
    }

    private int writeSummaryRow(Sheet sheet, int rowIndex, Styles styles, String label, Number value, boolean currency) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.boldStyle());

        if (currency) {
            applyCurrency(row.createCell(1), value instanceof BigDecimal bd ? bd : BigDecimal.valueOf(value.doubleValue()), styles.currencyStyle());
        } else {
            row.createCell(1).setCellValue(value.longValue());
        }
        return rowIndex + 1;
    }

    private void applyCurrency(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellValue(nullSafe(value).doubleValue());
        cell.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String mapReturnStatusVi(ReturnRequestStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case REQUESTED -> "Yêu cầu trả hàng";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
            case IN_TRANSIT -> "Đang gửi hàng hoàn";
            case RECEIVED -> "Đã nhận hàng hoàn";
            case QC_PASSED -> "QC đạt";
            case QC_FAILED -> "QC không đạt";
            case REFUND_PENDING -> "Chờ hoàn tiền";
            case REFUNDED -> "Đã hoàn tiền";
            case CANCELLED -> "Đã hủy yêu cầu";
            case CLOSED -> "Đã đóng";
        };
    }

    private String mapRefundStatusVi(RefundStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Chờ hoàn tiền";
            case PROCESSING -> "Đang xử lý";
            case SUCCESS -> "Hoàn tiền thành công";
            case FAILED -> "Hoàn tiền thất bại";
            case REVERSED -> "Hoàn tiền bị đảo ngược";
        };
    }

    private record Styles(
            CellStyle headerStyle,
            CellStyle titleStyle,
            CellStyle subtitleStyle,
            CellStyle boldStyle,
            CellStyle currencyStyle,
            CellStyle boldCurrencyStyle
    ) {
    }

    private record RevenueReportData(
            BigDecimal totalRevenue,
            long totalOrders,
            List<RevenueChartItem> revenueChart,
            List<TopProductItem> topProducts,
            List<TopVariantItem> topVariants,
            List<TopCustomerItem> topCustomers,
            List<TopCategoryItem> topCategories
    ) {
    }

    private record VoucherReportData(
            long voucherCount,
            long orderCount,
            BigDecimal totalProductDiscount,
            BigDecimal totalShippingDiscount,
            BigDecimal totalDiscount,
            List<VoucherUsageDetail> details
    ) {
    }

    private record VoucherUsageDetail(
            String code,
            String categoryLabel,
            long orderCount,
            BigDecimal totalDiscount,
            BigDecimal averageDiscount,
            int usedCount,
            String usageLimitLabel,
            String statusLabel,
            String startDateLabel,
            String endDateLabel
    ) {
    }

    private static final class VoucherAccumulator {
        private final String code;
        private CouponCategory category;
        private long orderCount;
        private BigDecimal totalDiscount = BigDecimal.ZERO;

        private VoucherAccumulator(String code) {
            this.code = code;
        }

        private String code() {
            return code;
        }

        private void recordUsage(CouponCategory voucherCategory, BigDecimal discount) {
            if (category == null) {
                category = voucherCategory;
            }
            orderCount++;
            totalDiscount = totalDiscount.add(discount == null ? BigDecimal.ZERO : discount);
        }

        private VoucherUsageDetail toDetail(Coupon coupon) {
            CouponCategory resolvedCategory = coupon != null && coupon.getCouponCategory() != null
                    ? coupon.getCouponCategory()
                    : category;
            BigDecimal average = orderCount == 0
                    ? BigDecimal.ZERO
                    : totalDiscount.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);

            return new VoucherUsageDetail(
                    code,
                    resolvedCategory == CouponCategory.SHIPPING ? "Freeship" : "Sản phẩm",
                    orderCount,
                    totalDiscount,
                    average,
                    coupon != null && coupon.getUsedCount() != null ? coupon.getUsedCount() : 0,
                    coupon != null && coupon.getUsageLimit() != null ? String.valueOf(coupon.getUsageLimit()) : "∞",
                    coupon != null && coupon.getStatus() != null ? coupon.getStatus().getDescription() : "",
                    coupon != null && coupon.getStartDate() != null ? coupon.getStartDate().format(DATE_FMT) : "",
                    coupon != null && coupon.getEndDate() != null ? coupon.getEndDate().format(DATE_FMT) : ""
            );
        }
    }
}
