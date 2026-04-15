package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.CategorySpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategorySpecAttributeRepository extends JpaRepository<CategorySpecAttribute, UUID> {
    List<CategorySpecAttribute> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);
}

