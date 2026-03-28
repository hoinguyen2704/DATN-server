package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    // Public vouchers: đang active, chưa hết hạn, isPublic = true
    List<Coupon> findByIsPublicTrueAndStatusAndEndDateAfter(String status, LocalDateTime now);

    List<Coupon> findByIsPublicTrueAndStatusAndEndDateIsNull(String status);
}
