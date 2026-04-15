package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, UUID> {

    @Query("SELECT COUNT(ri) > 0 FROM ReturnItem ri JOIN ri.returnRequest rr " +
            "WHERE ri.orderItem.id = :orderItemId " +
            "AND rr.status NOT IN (" +
            "com.hoz.hozitech.domain.enums.ReturnRequestStatus.REJECTED, " +
            "com.hoz.hozitech.domain.enums.ReturnRequestStatus.CANCELLED, " +
            "com.hoz.hozitech.domain.enums.ReturnRequestStatus.CLOSED)")
    boolean existsInNonRejectedRequest(@Param("orderItemId") UUID orderItemId);

    @Query("""
            SELECT ri.orderItem.variant.id, COALESCE(SUM(COALESCE(ri.approvedQuantity, ri.requestedQuantity)), 0)
            FROM ReturnItem ri
            JOIN ri.returnRequest rr
            WHERE rr.status IN (
                com.hoz.hozitech.domain.enums.ReturnRequestStatus.REFUNDED,
                com.hoz.hozitech.domain.enums.ReturnRequestStatus.CLOSED
            )
            AND ri.orderItem.variant.id IN :variantIds
            GROUP BY ri.orderItem.variant.id
            """)
    List<Object[]> sumReturnedQuantityByVariantIds(@Param("variantIds") List<UUID> variantIds);

    @Query("""
            SELECT ri.orderItem.variant.id, COALESCE(SUM(COALESCE(ri.approvedQuantity, ri.requestedQuantity)), 0)
            FROM ReturnItem ri
            JOIN ri.returnRequest rr
            WHERE rr.status IN (
                com.hoz.hozitech.domain.enums.ReturnRequestStatus.REFUNDED,
                com.hoz.hozitech.domain.enums.ReturnRequestStatus.CLOSED
            )
            AND rr.createdAt >= :from
            AND rr.createdAt <= :to
            AND ri.orderItem.variant.id IN :variantIds
            GROUP BY ri.orderItem.variant.id
            """)
    List<Object[]> sumReturnedQuantityByVariantIdsBetween(
            @Param("variantIds") List<UUID> variantIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
