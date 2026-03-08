package com.hoz.hozitech.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
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
        @Index(name = "idx_product_brand", columnList = "brand")
})
public class Product extends AbstractAuditingEntity {

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 300)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

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
}
