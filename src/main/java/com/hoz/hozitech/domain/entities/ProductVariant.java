package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(columnNames = "sku")
})
public class ProductVariant extends AbstractAuditingEntity {

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "variant_name", nullable = false, length = 255)
    private String variantName;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "capacity", length = 50)
    private String capacity;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 15, scale = 2)
    private BigDecimal compareAtPrice;

    @Builder.Default
    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private Integer stock = 0;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Boolean active = Boolean.TRUE;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder.Default
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}
