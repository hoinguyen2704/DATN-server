package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    Page<Wishlist> findByUserId(UUID userId, Pageable pageable);
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);
    void deleteByUserIdAndProductId(UUID userId, UUID productId);
    long countByUserId(UUID userId);
}
