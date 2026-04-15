package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
