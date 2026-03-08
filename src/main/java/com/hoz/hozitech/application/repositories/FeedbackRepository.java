package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    Page<Feedback> findByProductId(UUID productId, Pageable pageable);

    Page<Feedback> findByProductIdAndStar(UUID productId, Integer star, Pageable pageable);

    boolean existsByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);
}
