package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.CategoryVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryVariantAttributeRepository extends JpaRepository<CategoryVariantAttribute, UUID> {
    List<CategoryVariantAttribute> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    @Query("""
            select distinct mapping
            from CategoryVariantAttribute mapping
            join fetch mapping.variantAttribute attribute
            left join fetch attribute.options
            where mapping.category.id = :categoryId
            order by mapping.sortOrder asc
            """)
    List<CategoryVariantAttribute> findSchemaByCategoryId(@Param("categoryId") UUID categoryId);
}
