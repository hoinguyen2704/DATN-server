package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import com.hoz.hozitech.domain.enums.CouponStatus;
import com.hoz.hozitech.domain.enums.DiscountType;
import com.hoz.hozitech.domain.enums.CouponCategory;
import com.hoz.hozitech.domain.enums.CouponApplyType;
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

        @Enumerated(EnumType.STRING)
        @Column(name = "discount_type", nullable = false, length = 50)
        private DiscountType discountType;

        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "coupon_category", length = 30, columnDefinition = "varchar(30) default 'PRODUCT'")
        private CouponCategory couponCategory = CouponCategory.PRODUCT;

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
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 50)
        private CouponStatus status = CouponStatus.ACTIVE;

        // NEW: Visibility
        @Builder.Default
        @Column(name = "is_public", nullable = false)
        private Boolean isPublic = false;

        // NEW: Apply scope
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "apply_type", nullable = false, length = 30)
        private CouponApplyType applyType = CouponApplyType.ALL;

        // NEW: Applicable products (only when applyType = SPECIFIC_PRODUCTS)
        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "coupon_applicable_products", joinColumns = @JoinColumn(name = "coupon_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
        @Builder.Default
        private List<Product> applicableProducts = new ArrayList<>();
}
