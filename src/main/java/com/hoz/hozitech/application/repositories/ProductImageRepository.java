package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    // Tất cả ảnh của sản phẩm (chung + các variant)
    List<ProductImage> findByProductIdOrderBySortOrder(UUID productId);

    // Ảnh chung của sản phẩm (không thuộc variant nào)
    List<ProductImage> findByProductIdAndVariantIsNullOrderBySortOrder(UUID productId);

    // Ảnh riêng của 1 variant
    List<ProductImage> findByVariantIdOrderBySortOrder(UUID variantId);

    @Query("""
            select pi
            from ProductImage pi
            where pi.variant.id in :variantIds
            order by pi.variant.id asc,
              case when pi.isPrimary = true then 0 else 1 end,
              pi.sortOrder asc,
              pi.id asc
            """)
    List<ProductImage> findByVariantIdInOrderByPreferred(@Param("variantIds") Collection<UUID> variantIds);

    @Query("""
            select pi
            from ProductImage pi
            where pi.product.id in :productIds
            order by pi.product.id asc,
              case when pi.variant is null then 0 else 1 end,
              case when pi.isPrimary = true then 0 else 1 end,
              pi.sortOrder asc,
              pi.id asc
            """)
    List<ProductImage> findPreferredImagesByProductIds(@Param("productIds") List<UUID> productIds);

    default Map<UUID, String> findPreferredImageMapByProductIds(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> imageByProductId = new LinkedHashMap<>();
        for (ProductImage image : findPreferredImagesByProductIds(productIds)) {
            if (image.getProduct() == null || image.getProduct().getId() == null) {
                continue;
            }
            imageByProductId.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return imageByProductId;
    }

    void deleteAllByProductId(UUID productId);
}
