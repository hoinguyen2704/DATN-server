package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.FlashSaleItem;
import jakarta.persistence.LockModeType;
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

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    List<FlashSaleItem> findByFlashSaleId(UUID flashSaleId);

    @Query("SELECT fsi FROM FlashSaleItem fsi " +
            "JOIN fsi.flashSale fs " +
            "WHERE fsi.variant.id = :variantId " +
            "AND fs.status = 'ACTIVE' " +
            "AND fs.startTime <= CURRENT_TIMESTAMP AND fs.endTime >= CURRENT_TIMESTAMP " +
            "AND fsi.soldCount < fsi.flashStock")
    Optional<FlashSaleItem> findActiveFlashSaleItemByVariantId(@Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fsi FROM FlashSaleItem fsi " +
            "JOIN fsi.flashSale fs " +
            "WHERE fsi.variant.id = :variantId " +
            "AND fs.status = 'ACTIVE' " +
            "AND fs.startTime <= CURRENT_TIMESTAMP AND fs.endTime >= CURRENT_TIMESTAMP " +
            "AND fsi.soldCount < fsi.flashStock")
    Optional<FlashSaleItem> findActiveFlashSaleItemByVariantIdForUpdate(@Param("variantId") UUID variantId);

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
}
