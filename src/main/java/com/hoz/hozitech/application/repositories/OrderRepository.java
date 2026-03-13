package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    BigDecimal getTotalRevenue();

    // --- Dashboard Statistics ---

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'SHIPPED' AND o.createdAt >= :from AND o.createdAt <= :to")
    BigDecimal sumRevenueByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :from AND o.createdAt <= :to")
    long countOrdersByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = :status AND o.createdAt >= :from AND o.createdAt <= :to")
    long countOrdersByStatusAndDateRange(@Param("status") OrderStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT CAST(o.createdAt AS date) AS day, COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
            "FROM Order o WHERE o.orderStatus = 'SHIPPED' AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY CAST(o.createdAt AS date) ORDER BY day")
    List<Object[]> findRevenueGroupedByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT EXTRACT(MONTH FROM o.createdAt) AS month, COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
            "FROM Order o WHERE o.orderStatus = 'SHIPPED' AND EXTRACT(YEAR FROM o.createdAt) = :year " +
            "GROUP BY EXTRACT(MONTH FROM o.createdAt) ORDER BY month")
    List<Object[]> findRevenueGroupedByMonth(@Param("year") int year);

    @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("SELECT o.user.id, o.user.fullName, o.user.email, COUNT(o), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o WHERE o.orderStatus = 'SHIPPED' AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY o.user.id, o.user.fullName, o.user.email ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}

