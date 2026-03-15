package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.product.id = :productId")
    Double getAverageRating(@Param("productId") UUID productId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.product.id = :productId")
    Long countFeedbacks(@Param("productId") UUID productId);

    // Homepage queries
    List<Product> findByStatusAndIsFeaturedTrue(String status, Pageable pageable);

    List<Product> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN Feedback f ON f.product = p WHERE p.status = 'ACTIVE' GROUP BY p ORDER BY COALESCE(AVG(f.rating), 0) DESC, COUNT(f) DESC")
    List<Product> findTopRatedProducts(Pageable pageable);
}
