package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Coupon;
import jakarta.persistence.LockModeType;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);

    // Public vouchers: đang active, chưa hết hạn, isPublic = true
    List<Coupon> findByIsPublicTrueAndStatusAndEndDateAfter(CouponStatus status, LocalDateTime now);

    List<Coupon> findByIsPublicTrueAndStatusAndEndDateIsNull(CouponStatus status);

    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) IN :codes")
    List<Coupon> findAllByUpperCodeIn(@Param("codes") Collection<String> codes);
}
