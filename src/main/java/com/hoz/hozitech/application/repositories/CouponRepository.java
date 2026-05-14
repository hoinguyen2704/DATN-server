package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hoz.hozitech.domain.enums.CouponStatus;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    Page<Coupon> findByCodeContainingIgnoreCase(String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);

    // Public vouchers: đang active, chưa hết hạn, isPublic = true
    List<Coupon> findByIsPublicTrueAndStatusAndEndDateAfter(CouponStatus status, LocalDateTime now);

    List<Coupon> findByIsPublicTrueAndStatusAndEndDateIsNull(CouponStatus status);

    @Query("""
            select c
            from Coupon c
            where c.isPublic = true
              and c.status = :status
              and (c.startDate is null or c.startDate <= :now)
              and (c.endDate is null or c.endDate > :now)
            order by case when c.endDate is null then 1 else 0 end, c.endDate asc, c.createdAt desc
            """)
    List<Coupon> findVisiblePublicCoupons(@Param("status") CouponStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) IN :codes")
    List<Coupon> findAllByUpperCodeIn(@Param("codes") Collection<String> codes);

    @Query("""
            select c.id, p.id
            from Coupon c
            join c.applicableProducts p
            where c.id in :couponIds
            """)
    List<Object[]> findApplicableProductPairsByCouponIds(@Param("couponIds") Collection<UUID> couponIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Coupon c SET c.status = com.hoz.hozitech.domain.enums.CouponStatus.EXPIRED " +
            "WHERE c.status <> com.hoz.hozitech.domain.enums.CouponStatus.EXPIRED " +
            "AND c.endDate IS NOT NULL AND c.endDate < :now")
    int markExpiredCoupons(@Param("now") LocalDateTime now);
}
