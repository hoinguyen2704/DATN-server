package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Brand> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
