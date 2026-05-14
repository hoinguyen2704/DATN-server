package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.CategorySpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategorySpecAttributeRepository extends JpaRepository<CategorySpecAttribute, UUID> {
    List<CategorySpecAttribute> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    @Query("""
            select mapping
            from CategorySpecAttribute mapping
            join fetch mapping.specAttribute
            where mapping.category.id = :categoryId
            order by mapping.sortOrder asc
            """)
    List<CategorySpecAttribute> findSchemaByCategoryId(@Param("categoryId") UUID categoryId);
}
