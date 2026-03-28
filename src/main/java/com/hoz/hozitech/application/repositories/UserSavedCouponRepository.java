package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.UserSavedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSavedCouponRepository extends JpaRepository<UserSavedCoupon, UUID> {

    List<UserSavedCoupon> findByUserId(UUID userId);

    Optional<UserSavedCoupon> findByUserIdAndCouponId(UUID userId, UUID couponId);

    boolean existsByUserIdAndCouponId(UUID userId, UUID couponId);

    void deleteByUserIdAndCouponId(UUID userId, UUID couponId);

    List<UserSavedCoupon> findByUserIdAndCouponIdIn(UUID userId, List<UUID> couponIds);
}
