package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.FlashSaleItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    List<FlashSaleItem> findByFlashSaleId(UUID flashSaleId);

    void deleteByVariantIdIn(Collection<UUID> variantIds);

    @Query("""
            select fs.id as flashSaleId,
                   fsi.id as id,
                   p.id as productId,
                   p.slug as productSlug,
                   p.name as productName,
                   pv.id as variantId,
                   pv.variantName as variantName,
                   COALESCE(pv.compareAtPrice, p.originPrice, pv.price) as originalPrice,
                   pv.stock as stockQuantity,
                   fsi.flashPrice as flashPrice,
                   fsi.flashStock as flashStock,
                   fsi.soldCount as soldCount
            from FlashSaleItem fsi
            join fsi.flashSale fs
            join fsi.variant pv
            join pv.product p
            where fs.id in :flashSaleIds
            order by fs.endTime asc, fsi.createdAt asc, fsi.id asc
            """)
    List<StorefrontFlashSaleItemView> findStorefrontItemsByFlashSaleIds(
            @Param("flashSaleIds") Collection<UUID> flashSaleIds);

    @Query(
            value = """
                    select fs.id as flashSaleId,
                           fsi.id as id,
                           p.id as productId,
                           p.slug as productSlug,
                           p.name as productName,
                           pv.id as variantId,
                           pv.variantName as variantName,
                           COALESCE(pv.compareAtPrice, p.originPrice, pv.price) as originalPrice,
                           pv.stock as stockQuantity,
                           fsi.flashPrice as flashPrice,
                           fsi.flashStock as flashStock,
                           fsi.soldCount as soldCount
                    from FlashSaleItem fsi
                    join fsi.flashSale fs
                    join fsi.variant pv
                    join pv.product p
                    where fs.id = :flashSaleId
                      and fs.startTime <= CURRENT_TIMESTAMP
                      and fs.endTime >= CURRENT_TIMESTAMP
                    order by fsi.createdAt asc, fsi.id asc
                    """,
            countQuery = """
                    select count(fsi)
                    from FlashSaleItem fsi
                    join fsi.flashSale fs
                    where fs.id = :flashSaleId
                      and fs.startTime <= CURRENT_TIMESTAMP
                      and fs.endTime >= CURRENT_TIMESTAMP
                    """)
    Page<StorefrontFlashSaleItemView> findActiveStorefrontItemsByFlashSaleId(
            @Param("flashSaleId") UUID flashSaleId,
            Pageable pageable);

    @Query("""
            select fs.id as flashSaleId,
                   fsi.id as id,
                   p.id as productId,
                   p.slug as productSlug,
                   p.name as productName,
                   pv.id as variantId,
                   pv.variantName as variantName,
                   COALESCE(pv.compareAtPrice, p.originPrice, pv.price) as originalPrice,
                   pv.stock as stockQuantity,
                   fsi.flashPrice as flashPrice,
                   fsi.flashStock as flashStock,
                   fsi.soldCount as soldCount
            from FlashSaleItem fsi
            join fsi.flashSale fs
            join fsi.variant pv
            join pv.product p
            where pv.id in :variantIds
              and fs.startTime <= CURRENT_TIMESTAMP
              and fs.endTime >= CURRENT_TIMESTAMP
              and fsi.soldCount < fsi.flashStock
            order by fs.endTime asc, fsi.flashPrice asc, fsi.createdAt asc, fsi.id asc
            """)
    List<StorefrontFlashSaleItemView> findActiveStorefrontItemsByVariantIds(
            @Param("variantIds") Collection<UUID> variantIds);

    @Query("SELECT fsi FROM FlashSaleItem fsi " +
            "JOIN fsi.flashSale fs " +
            "WHERE fsi.variant.id = :variantId " +
            "AND fs.startTime <= CURRENT_TIMESTAMP AND fs.endTime >= CURRENT_TIMESTAMP " +
            "AND fsi.soldCount < fsi.flashStock " +
            "ORDER BY fsi.flashPrice ASC")
    List<FlashSaleItem> findActiveFlashSaleItemByVariantId(@Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fsi FROM FlashSaleItem fsi " +
            "JOIN fsi.flashSale fs " +
            "WHERE fsi.variant.id = :variantId " +
            "AND fs.startTime <= CURRENT_TIMESTAMP AND fs.endTime >= CURRENT_TIMESTAMP " +
            "AND fsi.soldCount < fsi.flashStock " +
            "ORDER BY fsi.flashPrice ASC")
    List<FlashSaleItem> findActiveFlashSaleItemByVariantIdForUpdate(@Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fsi FROM FlashSaleItem fsi " +
            "JOIN fsi.flashSale fs " +
            "WHERE fsi.variant.id = :variantId " +
            "AND fsi.flashPrice = :soldUnitPrice " +
            "AND fs.startTime <= :soldAt " +
            "AND fs.endTime >= :soldAt " +
            "ORDER BY fs.startTime DESC")
    List<FlashSaleItem> findRollbackCandidatesForUpdate(
            @Param("variantId") UUID variantId,
            @Param("soldUnitPrice") BigDecimal soldUnitPrice,
            @Param("soldAt") LocalDateTime soldAt);

    interface StorefrontFlashSaleItemView {
        UUID getFlashSaleId();

        UUID getId();

        UUID getProductId();

        String getProductSlug();

        String getProductName();

        UUID getVariantId();

        String getVariantName();

        BigDecimal getOriginalPrice();

        BigDecimal getFlashPrice();

        Integer getFlashStock();

        Integer getSoldCount();

        Integer getStockQuantity();
    }
}
