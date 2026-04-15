package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.SpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpecAttributeRepository extends JpaRepository<SpecAttribute, UUID> {
    Optional<SpecAttribute> findByNameIgnoreCase(String name);
    Optional<SpecAttribute> findByCodeIgnoreCase(String code);
}
