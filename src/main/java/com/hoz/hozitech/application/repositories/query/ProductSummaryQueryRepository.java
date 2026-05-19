package com.hoz.hozitech.application.repositories.query;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.domain.enums.ProductStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductSummaryQueryRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Page<PublicProductSummaryRow> findPublicProductSummaries(
            String keyword,
            String categorySlug,
            String brandSlug,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        int safePage = Math.max(page, 1);
        int safeSize = PaginationConstant.validateSize(size);
        boolean includeSoldMetrics = "popular".equalsIgnoreCase(sortBy);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        String whereClause = buildPublicProductSummaryWhereClause(keyword, categorySlug, brandSlug, false);
        MapSqlParameterSource params = buildPublicProductSummaryParams(
                keyword,
                categorySlug,
                brandSlug,
                safeSize,
                (safePage - 1L) * safeSize);

        long total = count(buildPublicProductSummaryCountSql(whereClause), params);
        if (total <= 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<PublicProductSummaryRow> rows = namedParameterJdbcTemplate.query(
                buildPublicProductSummaryDataSql(
                        resolvePublicProductOrderBy(sortBy, sortDir, includeSoldMetrics),
                        whereClause,
                        includeSoldMetrics),
                params,
                publicProductSummaryRowMapper());
        return new PageImpl<>(rows, pageable, total);
    }

    public List<PublicProductSummaryRow> findPublicProductSummaries(
            String keyword,
            String categorySlug,
            String brandSlug,
            boolean featuredOnly,
            int limit,
            boolean includeSoldMetrics,
            String sortBy,
            String sortDir) {
        int safeLimit = Math.max(1, PaginationConstant.validateSize(limit));
        String whereClause = buildPublicProductSummaryWhereClause(keyword, categorySlug, brandSlug, featuredOnly);
        MapSqlParameterSource params = buildPublicProductSummaryParams(
                keyword,
                categorySlug,
                brandSlug,
                safeLimit,
                0);

        return namedParameterJdbcTemplate.query(
                buildPublicProductSummaryDataSql(
                        resolvePublicProductOrderBy(sortBy, sortDir, includeSoldMetrics),
                        whereClause,
                        includeSoldMetrics),
                params,
                publicProductSummaryRowMapper());
    }

    public Page<ProductAdminSummaryRow> findAdminProductSummaries(
            String keyword,
            UUID categoryId,
            UUID brandId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        int safePage = Math.max(page, 1);
        int safeSize = PaginationConstant.validateSize(size);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        MapSqlParameterSource params = buildAdminProductSummaryParams(
                keyword,
                categoryId,
                brandId,
                status,
                safeSize,
                (safePage - 1L) * safeSize);

        String whereClause = buildAdminProductSummaryWhereClause(keyword, categoryId, brandId, status);
        long total = count(buildAdminProductSummaryCountSql(whereClause), params);
        if (total <= 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ProductAdminSummaryRow> rows = namedParameterJdbcTemplate.query(
                buildAdminProductSummaryDataSql(resolveAdminProductOrderBy(sortBy, sortDir), whereClause),
                params,
                productAdminSummaryRowMapper());
        return new PageImpl<>(rows, pageable, total);
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long total = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    private String buildPublicProductSummaryWhereClause(
            String keyword,
            String categorySlug,
            String brandSlug,
            boolean featuredOnly) {
        List<String> conditions = new ArrayList<>();
        conditions.add("p.status = 'ACTIVE'");

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            conditions.add("""
                    (
                        LOWER(p.name) LIKE :keyword
                        OR LOWER(COALESCE(p.description, '')) LIKE :keyword
                        OR LOWER(COALESCE(b.name, '')) LIKE :keyword
                    )
                    """);
        }

        String normalizedCategorySlug = normalizeOptionalText(categorySlug);
        if (normalizedCategorySlug != null) {
            conditions.add("LOWER(c.slug) = :categorySlug");
        }

        String normalizedBrandSlug = normalizeOptionalText(brandSlug);
        if (normalizedBrandSlug != null) {
            conditions.add("LOWER(COALESCE(b.slug, '')) = :brandSlug");
        }

        if (featuredOnly) {
            conditions.add("p.is_featured = TRUE");
        }

        return String.join("\n  AND ", conditions);
    }

    private MapSqlParameterSource buildPublicProductSummaryParams(
            String keyword,
            String categorySlug,
            String brandSlug,
            int limit,
            long offset) {
        String normalizedKeyword = normalizeOptionalText(keyword);
        String normalizedCategorySlug = normalizeOptionalText(categorySlug);
        String normalizedBrandSlug = normalizeOptionalText(brandSlug);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        if (normalizedKeyword != null) {
            params.addValue("keyword", "%" + normalizedKeyword + "%");
        }
        if (normalizedCategorySlug != null) {
            params.addValue("categorySlug", normalizedCategorySlug);
        }
        if (normalizedBrandSlug != null) {
            params.addValue("brandSlug", normalizedBrandSlug);
        }
        return params;
    }

    private String buildPublicProductSummaryCountSql(String whereClause) {
        return """
                SELECT COUNT(*)
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                WHERE
                """
                + whereClause
                + "\n";
    }

    private String buildPublicProductSummaryDataSql(String orderByClause, String whereClause, boolean includeSoldMetrics) {
        String soldSelectClause = includeSoldMetrics
                ? "    COALESCE(sold.total_sold, 0) AS total_sold,\n"
                : "    0 AS total_sold,\n";
        String soldJoinClause = includeSoldMetrics
                ? """
                LEFT JOIN (
                    SELECT pv.product_id, COALESCE(SUM(oi.quantity), 0) AS total_sold
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    JOIN product_variants pv ON pv.id = oi.variant_id
                    WHERE o.order_status = 'SHIPPED'
                    GROUP BY pv.product_id
                ) sold ON sold.product_id = p.id
                """
                : "";

        return """
                SELECT
                    p.id,
                    p.name,
                    p.slug,
                    p.description,
                    p.product_code,
                    p.origin_price,
                    p.status,
                    p.is_featured,
                    p.created_at,
                    b.id AS brand_id,
                    b.name AS brand_name,
                    c.id AS category_id,
                    c.name AS category_name,
                    c.slug AS category_slug,
                    COALESCE(stock.total_stock, 0) AS total_stock,
                """
                + soldSelectClause
                + """
                    COALESCE(price.lowest_price, p.origin_price) AS lowest_price,
                    COALESCE(flash.flash_price, price.lowest_price, p.origin_price) AS effective_price,
                    CASE
                        WHEN flash.flash_price IS NOT NULL THEN flash.original_price
                        ELSE price.compare_at_price
                    END AS compare_at_price,
                    COALESCE(review_stats.average_rating, 0) AS average_rating,
                    COALESCE(review_stats.total_reviews, 0) AS total_reviews
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN LATERAL (
                    SELECT COALESCE(SUM(pv.stock_quantity), 0) AS total_stock
                    FROM product_variants pv
                    WHERE pv.product_id = p.id
                ) stock ON TRUE
                LEFT JOIN LATERAL (
                    SELECT pv.price AS lowest_price,
                           COALESCE(pv.compare_at_price, p.origin_price, pv.price) AS compare_at_price
                    FROM product_variants pv
                    WHERE pv.product_id = p.id
                      AND pv.status = TRUE
                    ORDER BY pv.price ASC, pv.id ASC
                    LIMIT 1
                ) price ON TRUE
                LEFT JOIN LATERAL (
                    SELECT fsi.flash_price,
                           COALESCE(pv.compare_at_price, p.origin_price, pv.price) AS original_price
                    FROM product_variants pv
                    JOIN flash_sale_items fsi ON fsi.variant_id = pv.id
                    JOIN flash_sales fs ON fs.id = fsi.flash_sale_id
                    WHERE pv.product_id = p.id
                      AND fs.status <> 'HIDDEN'
                      AND fs.start_time <= CURRENT_TIMESTAMP
                      AND fs.end_time >= CURRENT_TIMESTAMP
                      AND fsi.sold_count < fsi.flash_stock
                      AND pv.status = TRUE
                      AND pv.stock_quantity > 0
                    ORDER BY fsi.flash_price ASC, fs.end_time ASC, fsi.created_at ASC, fsi.id ASC
                    LIMIT 1
                ) flash ON TRUE
                """
                + soldJoinClause
                + """
                LEFT JOIN (
                    SELECT f.product_id,
                           COALESCE(AVG(f.rating), 0) AS average_rating,
                           COUNT(*) AS total_reviews
                    FROM feedbacks f
                    WHERE f.status = 'APPROVED'
                    GROUP BY f.product_id
                ) review_stats ON review_stats.product_id = p.id
                WHERE
                """
                + whereClause
                + "\nORDER BY\n"
                + orderByClause
                + "\nLIMIT :limit OFFSET :offset\n";
    }

    private String resolvePublicProductOrderBy(String sortBy, String sortDir, boolean includeSoldMetrics) {
        String direction = Sort.Direction.ASC.name().equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String stockPriority = "CASE WHEN COALESCE(stock.total_stock, 0) > 0 THEN 1 ELSE 0 END DESC";

        return switch (sortBy == null ? "" : sortBy) {
            case "popular" -> includeSoldMetrics
                    ? stockPriority + ", COALESCE(sold.total_sold, 0) DESC, p.created_at DESC, p.id DESC"
                    : stockPriority + ", p.created_at DESC, p.id DESC";
            case "originPrice", "price" -> stockPriority + ", COALESCE(flash.flash_price, price.lowest_price, p.origin_price) " + direction + ", p.created_at DESC, p.id DESC";
            case "averageRating" -> stockPriority + ", COALESCE(review_stats.average_rating, 0) " + direction + ", p.created_at DESC, p.id DESC";
            case "createdAt" -> stockPriority + ", p.created_at " + direction + ", p.id DESC";
            default -> stockPriority + ", p.created_at DESC, p.id DESC";
        };
    }

    private RowMapper<PublicProductSummaryRow> publicProductSummaryRowMapper() {
        return (rs, rowNum) -> new PublicProductSummaryRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                readUuid(rs, "brand_id"),
                rs.getString("brand_name"),
                readUuid(rs, "category_id"),
                rs.getString("category_name"),
                rs.getString("category_slug"),
                rs.getString("product_code"),
                rs.getBigDecimal("origin_price"),
                rs.getBigDecimal("lowest_price"),
                rs.getBigDecimal("effective_price"),
                rs.getBigDecimal("compare_at_price"),
                rs.getDouble("average_rating"),
                rs.getInt("total_reviews"),
                ProductStatus.valueOf(rs.getString("status")),
                rs.getObject("is_featured", Boolean.class),
                rs.getInt("total_sold"),
                rs.getInt("total_stock"),
                rs.getObject("created_at", LocalDateTime.class));
    }

    private String buildAdminProductSummaryWhereClause(
            String keyword,
            UUID categoryId,
            UUID brandId,
            String status) {
        List<String> conditions = new ArrayList<>();
        conditions.add("p.status <> 'ARCHIVED'");

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            conditions.add("""
                    (
                        LOWER(p.name) LIKE :keyword
                        OR LOWER(COALESCE(p.description, '')) LIKE :keyword
                        OR LOWER(COALESCE(b.name, '')) LIKE :keyword
                    )
                    """);
        }

        if (categoryId != null) {
            conditions.add("p.category_id = :categoryId");
        }

        if (brandId != null) {
            conditions.add("p.brand_id = :brandId");
        }

        String normalizedStatus = status == null ? null : status.trim();
        if (normalizedStatus != null && !normalizedStatus.isBlank()) {
            conditions.add("p.status = :status");
        }

        return String.join("\n  AND ", conditions);
    }

    private MapSqlParameterSource buildAdminProductSummaryParams(
            String keyword,
            UUID categoryId,
            UUID brandId,
            String status,
            int limit,
            long offset) {
        String normalizedKeyword = normalizeOptionalText(keyword);
        String normalizedStatus = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalizedStatus != null && normalizedStatus.isBlank()) {
            normalizedStatus = null;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        if (normalizedKeyword != null) {
            params.addValue("keyword", "%" + normalizedKeyword + "%");
        }
        if (categoryId != null) {
            params.addValue("categoryId", categoryId);
        }
        if (brandId != null) {
            params.addValue("brandId", brandId);
        }
        if (normalizedStatus != null) {
            params.addValue("status", normalizedStatus);
        }
        return params;
    }

    private String buildAdminProductSummaryCountSql(String whereClause) {
        return """
                SELECT COUNT(*)
                FROM products p
                LEFT JOIN brands b ON b.id = p.brand_id
                WHERE
                """
                + whereClause
                + "\n";
    }

    private String buildAdminProductSummaryDataSql(String orderByClause, String whereClause) {
        return """
                SELECT
                    p.id,
                    p.name,
                    p.slug,
                    p.product_code,
                    p.origin_price,
                    p.status,
                    p.is_featured,
                    p.created_at,
                    b.id AS brand_id,
                    b.name AS brand_name,
                    c.id AS category_id,
                    c.name AS category_name,
                    c.slug AS category_slug,
                    COALESCE(stock.total_stock, 0) AS total_stock,
                    COALESCE(sold.total_sold, 0) AS total_sold,
                    COALESCE(price.lowest_price, p.origin_price) AS lowest_price
                FROM products p
                JOIN categories c ON c.id = p.category_id
                LEFT JOIN brands b ON b.id = p.brand_id
                LEFT JOIN (
                    SELECT pv.product_id, COALESCE(SUM(pv.stock_quantity), 0) AS total_stock
                    FROM product_variants pv
                    GROUP BY pv.product_id
                ) stock ON stock.product_id = p.id
                LEFT JOIN (
                    SELECT pv.product_id, MIN(pv.price) AS lowest_price
                    FROM product_variants pv
                    WHERE pv.status = TRUE
                    GROUP BY pv.product_id
                ) price ON price.product_id = p.id
                LEFT JOIN (
                    SELECT pv.product_id, COALESCE(SUM(oi.quantity), 0) AS total_sold
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    JOIN product_variants pv ON pv.id = oi.variant_id
                    WHERE o.order_status = 'SHIPPED'
                    GROUP BY pv.product_id
                ) sold ON sold.product_id = p.id
                WHERE
                """
                + whereClause
                + "\nORDER BY\n"
                + orderByClause
                + "\nLIMIT :limit OFFSET :offset\n";
    }

    private String resolveAdminProductOrderBy(String sortBy, String sortDir) {
        String direction = Sort.Direction.ASC.name().equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String orderField = switch (sortBy == null ? "" : sortBy) {
            case "name" -> "p.name";
            case "originPrice" -> "p.origin_price";
            case "totalStock" -> "COALESCE(stock.total_stock, 0)";
            case "totalSold" -> "COALESCE(sold.total_sold, 0)";
            case "status" -> "p.status";
            case "createdAt" -> "p.created_at";
            default -> "p.created_at";
        };
        if ("p.created_at".equals(orderField)) {
            return "p.created_at " + direction + ", p.id DESC";
        }
        return orderField + " " + direction + ", p.created_at DESC, p.id DESC";
    }

    private RowMapper<ProductAdminSummaryRow> productAdminSummaryRowMapper() {
        return (rs, rowNum) -> new ProductAdminSummaryRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("slug"),
                readUuid(rs, "brand_id"),
                rs.getString("brand_name"),
                readUuid(rs, "category_id"),
                rs.getString("category_name"),
                rs.getString("category_slug"),
                rs.getString("product_code"),
                rs.getBigDecimal("origin_price"),
                rs.getBigDecimal("lowest_price"),
                ProductStatus.valueOf(rs.getString("status")),
                rs.getObject("is_featured", Boolean.class),
                rs.getInt("total_sold"),
                rs.getInt("total_stock"),
                rs.getObject("created_at", LocalDateTime.class));
    }

    private UUID readUuid(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : UUID.fromString(value);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public record PublicProductSummaryRow(
            UUID id,
            String name,
            String slug,
            String description,
            UUID brandId,
            String brandName,
            UUID categoryId,
            String categoryName,
            String categorySlug,
            String productCode,
            BigDecimal originPrice,
            BigDecimal lowestPrice,
            BigDecimal price,
            BigDecimal compareAtPrice,
            Double averageRating,
            Integer totalReviews,
            ProductStatus status,
            Boolean isFeatured,
            Integer totalSold,
            Integer totalStock,
            LocalDateTime createdAt) {
    }

    public record ProductAdminSummaryRow(
            UUID id,
            String name,
            String slug,
            UUID brandId,
            String brandName,
            UUID categoryId,
            String categoryName,
            String categorySlug,
            String productCode,
            BigDecimal originPrice,
            BigDecimal lowestPrice,
            ProductStatus status,
            Boolean isFeatured,
            Integer totalSold,
            Integer totalStock,
            LocalDateTime createdAt) {
    }
}
