package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.CategoryVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryVariantAttributeRepository extends JpaRepository<CategoryVariantAttribute, UUID> {
    List<CategoryVariantAttribute> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);
}

