package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hoz.hozitech.domain.enums.CouponStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    // Public vouchers: đang active, chưa hết hạn, isPublic = true
    List<Coupon> findByIsPublicTrueAndStatusAndEndDateAfter(CouponStatus status, LocalDateTime now);

    List<Coupon> findByIsPublicTrueAndStatusAndEndDateIsNull(CouponStatus status);
}
