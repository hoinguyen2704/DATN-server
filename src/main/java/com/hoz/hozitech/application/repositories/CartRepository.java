package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    List<Cart> findByUserId(UUID userId);

    Optional<Cart> findByUserIdAndVariantId(UUID userId, UUID variantId);

    void deleteAllByUserId(UUID userId);

    void deleteByVariantIdIn(Collection<UUID> variantIds);

    void deleteByUserIdAndVariantIdIn(UUID userId, Collection<UUID> variantIds);

    long countByUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Cart c WHERE c.variant.product.id = :productId")
    void deleteAllByProductId(@Param("productId") UUID productId);
}
