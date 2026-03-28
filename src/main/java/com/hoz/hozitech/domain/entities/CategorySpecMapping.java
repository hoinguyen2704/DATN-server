package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Pivot table linking categories to spec attributes (many-to-many).
 * Allows per-category sort order and optional custom hint override.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "category_spec_mappings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "spec_attribute_id"}))
public class CategorySpecMapping extends AbstractAuditingEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spec_attribute_id", nullable = false)
    private SpecAttribute specAttribute;

    @Column(name = "custom_hint", length = 255)
    private String customHint;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * Returns custom hint if set, otherwise falls back to spec attribute's default hint.
     */
    public String getEffectiveHint() {
        if (customHint != null && !customHint.isBlank()) return customHint;
        return specAttribute != null ? specAttribute.getDefaultHint() : null;
    }
}
