package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_spec_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "spec_attribute_id"}))
public class ProductSpecValue extends AbstractAuditingEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spec_attribute_id", nullable = false)
    private SpecAttribute specAttribute;

    @Column(name = "value_text", nullable = false, columnDefinition = "TEXT")
    private String valueText;
}

