package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.hoz.hozitech.domain.enums.FeedbackStatus;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID>, JpaSpecificationExecutor<Feedback> {

    Page<Feedback> findByProductId(UUID productId, Pageable pageable);

    Page<Feedback> findByProductIdAndRating(UUID productId, Integer rating, Pageable pageable);

    Page<Feedback> findByProductIdAndStatus(UUID productId, FeedbackStatus status, Pageable pageable);

    @Query("""
            SELECT f FROM Feedback f
            WHERE f.product.id = :productId
              AND f.status = :status
              AND (:rating IS NULL OR f.rating = :rating)
              AND (
                :hasComment IS NULL
                OR (:hasComment = TRUE AND LENGTH(TRIM(COALESCE(f.content, ''))) > 0)
                OR (:hasComment = FALSE AND LENGTH(TRIM(COALESCE(f.content, ''))) = 0)
              )
            """)
    Page<Feedback> findPublicByProductWithFilters(
            @Param("productId") UUID productId,
            @Param("status") FeedbackStatus status,
            @Param("rating") Integer rating,
            @Param("hasComment") Boolean hasComment,
            Pageable pageable);

    Page<Feedback> findByStatus(FeedbackStatus status, Pageable pageable);

    long countByProductIdAndStatus(UUID productId, FeedbackStatus status);

    boolean existsByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);
    
    List<Feedback> findAllByUserIdAndProductIdAndVariantIdAndOrderIdOrderByCreatedAtAsc(UUID userId, UUID productId, UUID variantId, UUID orderId);

    @Query("""
            SELECT f FROM Feedback f
            WHERE f.user.id = :userId
              AND f.product.id = :productId
              AND (:variantId IS NULL OR (f.variant IS NOT NULL AND f.variant.id = :variantId))
              AND (:orderId IS NULL OR (f.order IS NOT NULL AND f.order.id = :orderId))
            ORDER BY f.createdAt ASC
            """)
    List<Feedback> findAllByUserIdAndProductIdWithOptionalVariantIdAndOrderIdOrderByCreatedAtAsc(
            @Param("userId") UUID userId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("orderId") UUID orderId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    // --- Dashboard Statistics ---

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.createdAt >= :from AND f.createdAt <= :to")
    long countNewFeedbacks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f GROUP BY f.rating ORDER BY f.rating")
    List<Object[]> getRatingDistribution();

    @Query("""
            SELECT f.rating, COUNT(f)
            FROM Feedback f
            WHERE f.product.id = :productId
              AND f.status = :status
            GROUP BY f.rating
            """)
    List<Object[]> countRatingDistributionByProductIdAndStatus(
            @Param("productId") UUID productId,
            @Param("status") FeedbackStatus status);

    @Query("""
            SELECT COUNT(f)
            FROM Feedback f
            WHERE f.product.id = :productId
              AND f.status = :status
              AND LENGTH(TRIM(COALESCE(f.content, ''))) > 0
            """)
    long countWithContentByProductIdAndStatus(
            @Param("productId") UUID productId,
            @Param("status") FeedbackStatus status);
}
