package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByStatusTrue();

    List<Category> findByParentCategoryIsNullAndStatusTrue();

    List<Category> findByParentCategoryIdAndStatusTrue(UUID parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parentCategory IS NULL AND c.status = true")
    List<Category> findAllRootCategoriesWithChildren();

    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
