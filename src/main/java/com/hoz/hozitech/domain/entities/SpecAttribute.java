package com.hoz.hozitech.domain.entities;

import com.hoz.hozitech.domain.entities.base.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Master list of spec attributes (e.g. "Màn hình", "RAM", "CPU").
 * Shared across multiple categories via many-to-many relationship.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "spec_attributes")
public class SpecAttribute extends AbstractAuditingEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "default_hint", length = 255)
    private String defaultHint;

    @Builder.Default
    @OneToMany(mappedBy = "specAttribute")
    private List<CategorySpecMapping> categoryMappings = new ArrayList<>();
}
