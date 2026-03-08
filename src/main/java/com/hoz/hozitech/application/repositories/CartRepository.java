package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    List<Cart> findByUserId(UUID userId);

    Optional<Cart> findByUserIdAndVariantId(UUID userId, UUID variantId);

    void deleteAllByUserId(UUID userId);

    long countByUserId(UUID userId);
}
