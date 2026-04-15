package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.VariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, UUID> {
    Optional<VariantAttribute> findByCodeIgnoreCase(String code);
    Optional<VariantAttribute> findByNameIgnoreCase(String name);
}

