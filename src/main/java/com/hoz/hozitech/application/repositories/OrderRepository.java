package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(UUID userId, OrderStatus orderStatus);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status")
    long countByOrderStatus(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'SHIPPED'")
    java.math.BigDecimal getTotalRevenue();
}
