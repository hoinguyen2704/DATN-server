package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.VariantAttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VariantAttributeOptionRepository extends JpaRepository<VariantAttributeOption, UUID> {
    Optional<VariantAttributeOption> findByVariantAttributeIdAndCodeIgnoreCase(UUID variantAttributeId, String code);
}

