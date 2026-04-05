package com.hoz.hozitech.domain.entities;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import com.hoz.hozitech.domain.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_slug", columnList = "slug"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_brand", columnList = "brand_id")
})
public class Product extends AbstractAuditingEntity {

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 300)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "origin_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal originPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs_json", columnDefinition = "jsonb")
    private String specsJson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<Feedback> feedbacks = new ArrayList<>();

    // Computed columns for sorting
    @Formula("(CASE WHEN (SELECT COALESCE(SUM(v.stock_quantity), 0) FROM product_variants v WHERE v.product_id = id) > 0 THEN 1 ELSE 0 END)")
    private Integer hasStock;

    @Formula("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi INNER JOIN orders o ON oi.order_id = o.id INNER JOIN product_variants v ON oi.variant_id = v.id WHERE v.product_id = id AND o.order_status = 'SHIPPED')")
    private Integer totalSold;

    @Formula("(SELECT COALESCE(SUM(v.stock_quantity), 0) FROM product_variants v WHERE v.product_id = id)")
    private Integer totalStock;

    @Formula("(SELECT COALESCE(AVG(f.rating), 0) FROM feedbacks f WHERE f.product_id = id)")
    private Double averageRating;
}
