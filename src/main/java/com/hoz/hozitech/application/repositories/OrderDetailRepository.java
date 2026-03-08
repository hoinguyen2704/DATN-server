package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

    List<OrderDetail> findByOrderId(UUID orderId);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od " +
            "WHERE od.variant.product.id = :productId AND od.order.status = 'SHIPPED'")
    Long sumSoldQuantityByProductId(@Param("productId") UUID productId);
}
