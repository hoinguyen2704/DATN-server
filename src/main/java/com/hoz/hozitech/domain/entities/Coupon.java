package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
public class Coupon extends AbstractAuditingEntity {

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 50)
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    @Builder.Default
    @Column(name = "coupon_category", length = 30, columnDefinition = "varchar(30) default 'PRODUCT'")
    private String couponCategory = "PRODUCT"; // PRODUCT, SHIPPING

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Min(0)
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Min(0)
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    // ─── NEW: Visibility ─────────────────────────────────────────
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    // ─── NEW: Apply scope ────────────────────────────────────────
    @Builder.Default
    @Column(name = "apply_type", nullable = false, length = 30)
    private String applyType = "ALL"; // ALL | SPECIFIC_PRODUCTS

    // ─── NEW: Applicable products (only when applyType = SPECIFIC_PRODUCTS)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "coupon_applicable_products",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private List<Product> applicableProducts = new ArrayList<>();
}
