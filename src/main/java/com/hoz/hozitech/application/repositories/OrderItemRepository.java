package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderItem od " +
            "WHERE od.variant.product.id = :productId AND od.order.orderStatus = 'SHIPPED'")
    Long sumSoldQuantityByProductId(@Param("productId") UUID productId);
}
