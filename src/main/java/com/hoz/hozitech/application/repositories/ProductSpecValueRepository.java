package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ProductSpecValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductSpecValueRepository extends JpaRepository<ProductSpecValue, UUID> {
    List<ProductSpecValue> findByProductId(UUID productId);
}

