package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    // Tất cả ảnh của sản phẩm (chung + các variant)
    List<ProductImage> findByProductIdOrderBySortOrder(UUID productId);

    // Ảnh chung của sản phẩm (không thuộc variant nào)
    List<ProductImage> findByProductIdAndVariantIsNullOrderBySortOrder(UUID productId);

    // Ảnh riêng của 1 variant
    List<ProductImage> findByVariantIdOrderBySortOrder(UUID variantId);

    void deleteAllByProductId(UUID productId);
}
