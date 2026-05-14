package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ProductVariantAttributeValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, UUID> {
    List<ProductVariantAttributeValue> findByProductVariantId(UUID productVariantId);

    @EntityGraph(attributePaths = {"variantAttribute", "option"})
    List<ProductVariantAttributeValue> findByProductVariantIdIn(Collection<UUID> productVariantIds);
}
