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

    Page<Feedback> findByStatus(FeedbackStatus status, Pageable pageable);

    boolean existsByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);
    
    List<Feedback> findAllByUserIdAndProductIdAndVariantIdAndOrderIdOrderByCreatedAtAsc(UUID userId, UUID productId, UUID variantId, UUID orderId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    // --- Dashboard Statistics ---

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.createdAt >= :from AND f.createdAt <= :to")
    long countNewFeedbacks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f GROUP BY f.rating ORDER BY f.rating")
    List<Object[]> getRatingDistribution();
}

