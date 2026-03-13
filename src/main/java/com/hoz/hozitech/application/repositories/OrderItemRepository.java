package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderItem od " +
            "WHERE od.variant.product.id = :productId AND od.order.orderStatus = 'SHIPPED'")
    Long sumSoldQuantityByProductId(@Param("productId") UUID productId);

    // --- Dashboard Statistics ---

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "WHERE oi.order.orderStatus = 'SHIPPED' AND oi.order.createdAt >= :from AND oi.order.createdAt <= :to")
    long sumProductsSoldByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT oi.variant.product.id, oi.variant.product.name, '', " +
            "SUM(oi.quantity), COALESCE(SUM(oi.subtotal), 0) " +
            "FROM OrderItem oi WHERE oi.order.orderStatus = 'SHIPPED' " +
            "AND oi.order.createdAt >= :from AND oi.order.createdAt <= :to " +
            "GROUP BY oi.variant.product.id, oi.variant.product.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT oi.variant.product.category.id, oi.variant.product.category.name, " +
            "SUM(oi.quantity), COALESCE(SUM(oi.subtotal), 0) " +
            "FROM OrderItem oi WHERE oi.order.orderStatus = 'SHIPPED' " +
            "AND oi.order.createdAt >= :from AND oi.order.createdAt <= :to " +
            "GROUP BY oi.variant.product.category.id, oi.variant.product.category.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingCategories(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}

