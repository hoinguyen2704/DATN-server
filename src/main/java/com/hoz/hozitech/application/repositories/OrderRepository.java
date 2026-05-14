package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentMethod;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.variant", "orderItems.variant.product"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.variant", "orderItems.variant.product"})
    Page<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(UUID userId, OrderStatus orderStatus, Pageable pageable);

    @Query(
            value = """
                    SELECT o.id
                    FROM Order o
                    WHERE o.user.id = :userId
                    ORDER BY o.createdAt DESC, o.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(o)
                    FROM Order o
                    WHERE o.user.id = :userId
                    """)
    Page<UUID> findOrderIdsByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = """
                    SELECT o.id
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND o.orderStatus = :orderStatus
                    ORDER BY o.createdAt DESC, o.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(o)
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND o.orderStatus = :orderStatus
                    """)
    Page<UUID> findOrderIdsByUserIdAndOrderStatusOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable);

    @Query(
            value = """
                    SELECT o.id
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND LOWER(o.orderNumber) LIKE :keyword
                    ORDER BY o.createdAt DESC, o.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(o)
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND LOWER(o.orderNumber) LIKE :keyword
                    """)
    Page<UUID> findOrderIdsByUserIdAndOrderNumberLikeOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(
            value = """
                    SELECT o.id
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND o.orderStatus = :status
                      AND LOWER(o.orderNumber) LIKE :keyword
                    ORDER BY o.createdAt DESC, o.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(o)
                    FROM Order o
                    WHERE o.user.id = :userId
                      AND o.orderStatus = :status
                      AND LOWER(o.orderNumber) LIKE :keyword
                    """)
    Page<UUID> findOrderIdsByUserIdAndOrderStatusAndOrderNumberLikeOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.variant", "orderItems.variant.product"})
    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            left join fetch oi.variant v
            left join fetch v.product p
            where o.id in :orderIds
            """)
    List<Order> findUserOrderListByIds(@Param("orderIds") Collection<UUID> orderIds);

    @Query("""
            select o.id as orderId,
                   o.orderNumber as orderNumber,
                   o.orderStatus as orderStatus,
                   o.paymentMethod as paymentMethod,
                   o.paymentStatus as paymentStatus,
                   o.totalAmount as totalAmount,
                   u.fullName as customerName,
                   u.email as customerEmail,
                   u.phoneNumber as customerPhone,
                   o.createdAt as createdAt,
                   o.updatedAt as updatedAt,
                   oi.id as itemId,
                   v.id as variantId,
                   p.id as productId,
                   p.slug as productSlug,
                   oi.productName as productName,
                   oi.variantName as variantName,
                   v.sku as sku,
                   oi.unitPrice as unitPrice,
                   oi.quantity as quantity,
                   oi.subtotal as itemSubtotal
            from Order o
            join o.user u
            left join o.orderItems oi
            left join oi.variant v
            left join v.product p
            where o.id in :orderIds
            order by o.createdAt desc, o.id desc, oi.createdAt asc, oi.id asc
            """)
    List<UserOrderListRow> findUserOrderListRowsByIds(@Param("orderIds") Collection<UUID> orderIds);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);

    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Query("SELECT COUNT(o) > 0 FROM Order o " +
            "WHERE o.user.id = :userId " +
            "AND (" +
            "  (o.couponCode IS NOT NULL AND UPPER(o.couponCode) = UPPER(:couponCode)) " +
            "  OR (o.shippingCouponCode IS NOT NULL AND UPPER(o.shippingCouponCode) = UPPER(:couponCode))" +
            ")")
    boolean existsCouponUsedByUser(@Param("userId") UUID userId, @Param("couponCode") String couponCode);

    @Query("SELECT COUNT(o) > 0 FROM Order o " +
            "WHERE (" +
            "  (o.couponCode IS NOT NULL AND UPPER(o.couponCode) = UPPER(:couponCode)) " +
            "  OR (o.shippingCouponCode IS NOT NULL AND UPPER(o.shippingCouponCode) = UPPER(:couponCode))" +
            ")")
    boolean existsByCouponCodeInAnyOrder(@Param("couponCode") String couponCode);

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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = :payStatus AND o.createdAt >= :from AND o.createdAt <= :to")
    long countOrdersByPaymentStatusAndDateRange(@Param("payStatus") com.hoz.hozitech.domain.enums.PaymentStatus payStatus, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("SELECT o.user.id, o.user.fullName, o.user.email, COUNT(o), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o WHERE o.orderStatus = 'SHIPPED' AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY o.user.id, o.user.fullName, o.user.email ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /**
     * Finds PENDING orders with online payment methods (not COD) created before the cutoff time.
     * These are "zombie" orders where the customer abandoned checkout.
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status " +
            "AND o.paymentMethod <> :excludedPaymentMethod " +
            "AND o.createdAt < :cutoff")
    List<Order> findStalePendingOrders(
            @Param("status") OrderStatus status,
            @Param("excludedPaymentMethod") com.hoz.hozitech.domain.enums.PaymentMethod excludedPaymentMethod,
            @Param("cutoff") LocalDateTime cutoff);

    interface UserOrderListRow {
        UUID getOrderId();

        String getOrderNumber();

        OrderStatus getOrderStatus();

        PaymentMethod getPaymentMethod();

        PaymentStatus getPaymentStatus();

        BigDecimal getTotalAmount();

        String getCustomerName();

        String getCustomerEmail();

        String getCustomerPhone();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();

        UUID getItemId();

        UUID getVariantId();

        UUID getProductId();

        String getProductSlug();

        String getProductName();

        String getVariantName();

        String getSku();

        BigDecimal getUnitPrice();

        Integer getQuantity();

        BigDecimal getItemSubtotal();
    }
}
