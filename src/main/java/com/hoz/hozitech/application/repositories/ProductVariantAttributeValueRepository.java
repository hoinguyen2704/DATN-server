package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ProductVariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, UUID> {
    List<ProductVariantAttributeValue> findByProductVariantId(UUID productVariantId);
}

