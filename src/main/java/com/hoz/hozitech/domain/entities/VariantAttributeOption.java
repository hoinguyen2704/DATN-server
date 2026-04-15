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
@Table(name = "variant_attribute_options",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_attribute_id", "code"}))
public class VariantAttributeOption extends AbstractAuditingEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_attribute_id", nullable = false)
    private VariantAttribute variantAttribute;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}

