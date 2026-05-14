package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.domain.enums.ExportJobType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.hoz.hozitech.application.services.export.ExportHelpers.DATE_FMT;
import static com.hoz.hozitech.application.services.export.ExportHelpers.mapOrderStatusVi;

@Component
@RequiredArgsConstructor
public class ChunkedExcelExportGenerator {

    public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String ZIP_CONTENT_TYPE = "application/zip";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${app.export.rows-per-file:50000}")
    private int rowsPerFile;

    @Value("${app.export.batch-size:1000}")
    private int batchSize;

    public GeneratedExport generate(
            ExportJobType type,
            Map<String, Object> params,
            Path jobDirectory,
            ProgressListener progressListener) throws IOException {
        QuerySpec countQuery = buildQuery(type, params, 0, 0, true);
        long totalRows = jdbcTemplate.queryForObject(countQuery.sql(), countQuery.params(), Long.class);
        int safeRowsPerFile = Math.max(1, rowsPerFile);
        int partCount = Math.max(1, (int) Math.ceil((double) totalRows / safeRowsPerFile));
        String prefix = filePrefix(type);
        String dateStamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        List<Path> partFiles = new ArrayList<>();

        long processedRows = 0;
        for (int partIndex = 0; partIndex < partCount; partIndex++) {
            long partOffset = (long) partIndex * safeRowsPerFile;
            long partLimit = totalRows == 0 ? 0 : Math.min(safeRowsPerFile, totalRows - partOffset);
            String partSuffix = partCount == 1 ? "" : "_part_" + String.format("%03d", partIndex + 1);
            Path partPath = jobDirectory.resolve(prefix + "_" + dateStamp + partSuffix + ".xlsx");
            processedRows = writeWorkbook(type, params, partPath, partOffset, partLimit, processedRows, totalRows, progressListener);
            partFiles.add(partPath);
        }

        progressListener.onProgress(totalRows, totalRows);
        if (partFiles.size() == 1) {
            Path file = partFiles.get(0);
            return new GeneratedExport(file, file.getFileName().toString(), XLSX_CONTENT_TYPE, totalRows);
        }

        Path zipPath = jobDirectory.resolve(prefix + "_" + dateStamp + ".zip");
        zipFiles(partFiles, zipPath);
        for (Path partFile : partFiles) {
            Files.deleteIfExists(partFile);
        }
        return new GeneratedExport(zipPath, zipPath.getFileName().toString(), ZIP_CONTENT_TYPE, totalRows);
    }

    private long writeWorkbook(
            ExportJobType type,
            Map<String, Object> params,
            Path outputPath,
            long startOffset,
            long partLimit,
            long processedRows,
            long totalRows,
            ProgressListener progressListener) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
            Sheet sheet = workbook.createSheet(sheetName(type));
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            writeTitleAndHeader(sheet, headerStyle, type);
            ColumnDef[] columns = columns(type);
            int rowIndex = 4;
            int safeBatchSize = Math.max(1, batchSize);
            long writtenInPart = 0;

            while (writtenInPart < partLimit) {
                int limit = (int) Math.min(safeBatchSize, partLimit - writtenInPart);
                QuerySpec dataQuery = buildQuery(type, params, limit, startOffset + writtenInPart, false);
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery.sql(), dataQuery.params());
                if (rows.isEmpty()) {
                    break;
                }

                for (Map<String, Object> source : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    long rowNumber = startOffset + writtenInPart + 1;
                    writeDataRow(row, columns, source, rowNumber, type, currencyStyle);
                    writtenInPart++;
                    processedRows++;
                }
                progressListener.onProgress(processedRows, totalRows);

                if (rows.size() < limit) {
                    break;
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, columns[i].width());
            }
            workbook.write(outputStream);
            return processedRows;
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }

    private void writeTitleAndHeader(Sheet sheet, CellStyle headerStyle, ExportJobType type) {
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(title(type));
        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Ngày xuất: " + LocalDateTime.now().format(DATE_FMT));

        Row headerRow = sheet.createRow(3);
        ColumnDef[] columns = columns(type);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i].header());
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeDataRow(
            Row row,
            ColumnDef[] columns,
            Map<String, Object> source,
            long rowNumber,
            ExportJobType type,
            CellStyle currencyStyle) {
        for (int i = 0; i < columns.length; i++) {
            ColumnDef column = columns[i];
            Object value = "__index".equals(column.key()) ? rowNumber : source.get(column.key());
            Cell cell = row.createCell(i);
            if (column.currency()) {
                cell.setCellValue(toDouble(value));
                cell.setCellStyle(currencyStyle);
                continue;
            }
            cell.setCellValue(formatValue(type, column.key(), value));
        }
    }

    private String formatValue(ExportJobType type, String key, Object value) {
        if (value == null) {
            return "";
        }
        if (ExportJobType.ORDERS == type && "order_status".equals(key)) {
            return mapOrderStatusVi(String.valueOf(value));
        }
        if (ExportJobType.PRODUCTS == type && "status".equals(key)) {
            return "ACTIVE".equalsIgnoreCase(String.valueOf(value)) ? "Đang bán" : "Đã ẩn";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATE_FMT);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_FMT);
        }
        return String.valueOf(value);
    }

    private double toDouble(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0;
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        return headerStyle;
    }

    private void zipFiles(List<Path> files, Path zipPath) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path file : files) {
                zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private QuerySpec buildQuery(
            ExportJobType type,
            Map<String, Object> rawParams,
            int limit,
            long offset,
            boolean countOnly) {
        Map<String, Object> params = rawParams == null ? Map.of() : rawParams;
        return switch (type) {
            case ORDERS -> orderQuery(params, limit, offset, countOnly);
            case RETURNS -> returnQuery(params, limit, offset, countOnly);
            case PRODUCTS -> productQuery(params, limit, offset, countOnly);
            case FEEDBACKS -> feedbackQuery(params, limit, offset, countOnly);
            case USERS -> userQuery(params, limit, offset, countOnly);
        };
    }

    private QuerySpec orderQuery(Map<String, Object> rawParams, int limit, long offset, boolean countOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        addStringFilter(conditions, params, "status", rawParams, "o.order_status = :status", true);
        addKeywordFilter(conditions, params, rawParams,
                "LOWER(o.order_number) LIKE :keyword",
                "LOWER(COALESCE(u.full_name, '')) LIKE :keyword",
                "LOWER(COALESCE(u.email, '')) LIKE :keyword");
        addDateRange(conditions, params, rawParams, "o.created_at");

        String fromAndWhere = """
                FROM orders o
                LEFT JOIN users u ON u.id = o.user_id
                """
                + whereClause(conditions);
        String select = """
                SELECT o.order_number, u.full_name, u.email, u.phone_number,
                       o.subtotal, o.discount_amount, o.shipping_fee, o.tax_amount, o.total_amount,
                       o.order_status, o.payment_status, o.created_at
                """;
        return querySpec(select, fromAndWhere, "o.created_at DESC, o.id DESC", params, limit, offset, countOnly);
    }

    private QuerySpec returnQuery(Map<String, Object> rawParams, int limit, long offset, boolean countOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        addStringFilter(conditions, params, "status", rawParams, "rr.status = :status", true);
        addKeywordFilter(conditions, params, rawParams,
                "LOWER(rr.return_number) LIKE :keyword",
                "LOWER(COALESCE(o.order_number, '')) LIKE :keyword",
                "LOWER(COALESCE(u.full_name, '')) LIKE :keyword",
                "LOWER(COALESCE(u.email, '')) LIKE :keyword");

        String fromAndWhere = """
                FROM return_requests rr
                LEFT JOIN orders o ON o.id = rr.order_id
                LEFT JOIN users u ON u.id = rr.user_id
                """
                + whereClause(conditions);
        String select = """
                SELECT rr.return_number, o.order_number, u.full_name, u.email,
                       rr.reason, rr.requested_amount, rr.approved_amount, rr.refund_amount,
                       rr.status, rr.refund_status, rr.created_at, rr.resolved_at
                """;
        return querySpec(select, fromAndWhere, "rr.created_at DESC, rr.id DESC", params, limit, offset, countOnly);
    }

    private QuerySpec productQuery(Map<String, Object> rawParams, int limit, long offset, boolean countOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        addStringFilter(conditions, params, "status", rawParams, "p.status = :status", true);
        addUuidFilter(conditions, params, "categoryId", rawParams, "p.category_id = :categoryId");
        addKeywordFilter(conditions, params, rawParams,
                "LOWER(p.name) LIKE :keyword",
                "LOWER(p.slug) LIKE :keyword");

        String fromAndWhere = """
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN (
                    SELECT pv.product_id, COALESCE(SUM(pv.stock_quantity), 0) AS total_stock
                    FROM product_variants pv
                    GROUP BY pv.product_id
                ) stock ON stock.product_id = p.id
                LEFT JOIN (
                    SELECT pv.product_id, COALESCE(SUM(oi.quantity), 0) AS total_sold
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    JOIN product_variants pv ON pv.id = oi.variant_id
                    WHERE o.order_status = 'SHIPPED'
                    GROUP BY pv.product_id
                ) sold ON sold.product_id = p.id
                """
                + whereClause(conditions);
        String select = """
                SELECT p.name, p.slug, c.name AS category_name, b.name AS brand_name,
                       p.origin_price, COALESCE(stock.total_stock, 0) AS total_stock,
                       COALESCE(sold.total_sold, 0) AS total_sold, p.status, p.created_at
                """;
        return querySpec(select, fromAndWhere, "p.created_at DESC, p.id DESC", params, limit, offset, countOnly);
    }

    private QuerySpec feedbackQuery(Map<String, Object> rawParams, int limit, long offset, boolean countOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        addStringFilter(conditions, params, "status", rawParams, "f.status = :status", true);
        addUuidFilter(conditions, params, "productId", rawParams, "f.product_id = :productId");

        String fromAndWhere = """
                FROM feedbacks f
                LEFT JOIN products p ON p.id = f.product_id
                LEFT JOIN users u ON u.id = f.user_id
                """
                + whereClause(conditions);
        String select = """
                SELECT f.id, p.name AS product_name, u.full_name, u.email,
                       f.rating, f.content, f.status, f.admin_reply, f.created_at
                """;
        return querySpec(select, fromAndWhere, "f.created_at DESC, f.id DESC", params, limit, offset, countOnly);
    }

    private QuerySpec userQuery(Map<String, Object> rawParams, int limit, long offset, boolean countOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        addStringFilter(conditions, params, "role", rawParams, "CAST(r.id AS TEXT) = :role", true);
        addKeywordFilter(conditions, params, rawParams,
                "LOWER(COALESCE(u.full_name, '')) LIKE :keyword",
                "LOWER(COALESCE(u.email, '')) LIKE :keyword");

        String fromAndWhere = """
                FROM users u
                LEFT JOIN roles r ON r.id = u.role_id
                """
                + whereClause(conditions);
        String select = """
                SELECT u.id, u.full_name, u.email, u.phone_number, r.id AS role_id, u.created_at
                """;
        return querySpec(select, fromAndWhere, "u.created_at DESC, u.id DESC", params, limit, offset, countOnly);
    }

    private QuerySpec querySpec(
            String select,
            String fromAndWhere,
            String orderBy,
            MapSqlParameterSource params,
            int limit,
            long offset,
            boolean countOnly) {
        if (countOnly) {
            return new QuerySpec("SELECT COUNT(*)\n" + fromAndWhere, params);
        }
        params.addValue("limit", limit);
        params.addValue("offset", offset);
        return new QuerySpec(select + "\n" + fromAndWhere
                + "\nORDER BY " + orderBy
                + "\nLIMIT :limit OFFSET :offset", params);
    }

    private void addStringFilter(
            List<String> conditions,
            MapSqlParameterSource params,
            String paramName,
            Map<String, Object> rawParams,
            String condition,
            boolean uppercase) {
        String value = getString(rawParams, paramName);
        if (value == null) {
            return;
        }
        conditions.add(condition);
        params.addValue(paramName, uppercase ? value.toUpperCase(Locale.ROOT) : value);
    }

    private void addUuidFilter(
            List<String> conditions,
            MapSqlParameterSource params,
            String paramName,
            Map<String, Object> rawParams,
            String condition) {
        UUID value = getUuid(rawParams, paramName);
        if (value == null) {
            return;
        }
        conditions.add(condition);
        params.addValue(paramName, value);
    }

    private void addKeywordFilter(
            List<String> conditions,
            MapSqlParameterSource params,
            Map<String, Object> rawParams,
            String... expressions) {
        String keyword = getString(rawParams, "keyword");
        if (keyword == null) {
            return;
        }
        params.addValue("keyword", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        conditions.add("(" + String.join(" OR ", expressions) + ")");
    }

    private void addDateRange(
            List<String> conditions,
            MapSqlParameterSource params,
            Map<String, Object> rawParams,
            String column) {
        LocalDateTime from = getDateTime(rawParams, "from", false);
        LocalDateTime to = getDateTime(rawParams, "to", true);
        if (from != null) {
            conditions.add(column + " >= :fromDate");
            params.addValue("fromDate", from);
        }
        if (to != null) {
            conditions.add(column + " <= :toDate");
            params.addValue("toDate", to);
        }
    }

    private String whereClause(List<String> conditions) {
        return conditions.isEmpty() ? "" : "\nWHERE " + String.join("\n  AND ", conditions);
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private UUID getUuid(Map<String, Object> params, String key) {
        String value = getString(params, key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private LocalDateTime getDateTime(Map<String, Object> params, String key, boolean endOfDay) {
        String value = getString(params, key);
        if (value == null) {
            return null;
        }
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String title(ExportJobType type) {
        return switch (type) {
            case ORDERS -> "DANH SÁCH ĐƠN HÀNG";
            case RETURNS -> "BÁO CÁO ĐƠN HOÀN HỦY";
            case PRODUCTS -> "BÁO CÁO DANH SÁCH SẢN PHẨM";
            case FEEDBACKS -> "DANH SÁCH ĐÁNH GIÁ";
            case USERS -> "DANH SÁCH NGƯỜI DÙNG";
        };
    }

    private String sheetName(ExportJobType type) {
        return switch (type) {
            case ORDERS -> "Đơn hàng";
            case RETURNS -> "Đơn hoàn hủy";
            case PRODUCTS -> "Danh sách sản phẩm";
            case FEEDBACKS -> "Đánh giá";
            case USERS -> "Người dùng";
        };
    }

    private String filePrefix(ExportJobType type) {
        return switch (type) {
            case ORDERS -> "orders";
            case RETURNS -> "returns";
            case PRODUCTS -> "products";
            case FEEDBACKS -> "feedbacks";
            case USERS -> "users";
        };
    }

    private ColumnDef[] columns(ExportJobType type) {
        return switch (type) {
            case ORDERS -> new ColumnDef[]{
                    text("Mã đơn", "order_number", 18),
                    text("Khách hàng", "full_name", 28),
                    text("Email", "email", 28),
                    text("SĐT", "phone_number", 18),
                    money("Tạm tính", "subtotal"),
                    money("Giảm giá", "discount_amount"),
                    money("Phí ship", "shipping_fee"),
                    money("Thuế", "tax_amount"),
                    money("Thành tiền", "total_amount"),
                    text("Trạng thái", "order_status", 18),
                    text("Thanh toán", "payment_status", 18),
                    text("Ngày đặt", "created_at", 22)
            };
            case RETURNS -> new ColumnDef[]{
                    text("STT", "__index", 10),
                    text("Mã yêu cầu", "return_number", 22),
                    text("Mã đơn hàng", "order_number", 22),
                    text("Khách hàng", "full_name", 28),
                    text("Email", "email", 28),
                    text("Lý do", "reason", 42),
                    money("Số tiền yêu cầu", "requested_amount"),
                    money("Số tiền duyệt", "approved_amount"),
                    money("Số tiền hoàn", "refund_amount"),
                    text("Trạng thái", "status", 20),
                    text("Hoàn tiền", "refund_status", 18),
                    text("Ngày tạo", "created_at", 22),
                    text("Ngày xử lý", "resolved_at", 22)
            };
            case PRODUCTS -> new ColumnDef[]{
                    text("STT", "__index", 10),
                    text("Tên sản phẩm", "name", 42),
                    text("SKU/Slug", "slug", 35),
                    text("Danh mục", "category_name", 24),
                    text("Thương hiệu", "brand_name", 24),
                    money("Giá gốc", "origin_price"),
                    text("Tồn kho", "total_stock", 14),
                    text("Đã bán", "total_sold", 14),
                    text("Trạng thái", "status", 18),
                    text("Ngày tạo", "created_at", 22)
            };
            case FEEDBACKS -> new ColumnDef[]{
                    text("ID", "id", 38),
                    text("Sản phẩm", "product_name", 42),
                    text("Khách hàng", "full_name", 28),
                    text("Email", "email", 28),
                    text("Đánh giá", "rating", 12),
                    text("Nội dung", "content", 48),
                    text("Trạng thái", "status", 18),
                    text("Phản hồi admin", "admin_reply", 42),
                    text("Ngày tạo", "created_at", 22)
            };
            case USERS -> new ColumnDef[]{
                    text("ID", "id", 38),
                    text("Họ tên", "full_name", 28),
                    text("Email", "email", 28),
                    text("SĐT", "phone_number", 18),
                    text("Vai trò", "role_id", 18),
                    text("Ngày tạo", "created_at", 22)
            };
        };
    }

    private ColumnDef text(String header, String key, int widthChars) {
        return new ColumnDef(header, key, false, widthChars * 256);
    }

    private ColumnDef money(String header, String key) {
        return new ColumnDef(header, key, true, 18 * 256);
    }

    public interface ProgressListener {
        void onProgress(long processedRows, long totalRows);
    }

    public record GeneratedExport(Path path, String fileName, String contentType, long totalRows) {
    }

    private record ColumnDef(String header, String key, boolean currency, int width) {
    }

    private record QuerySpec(String sql, MapSqlParameterSource params) {
    }
}
