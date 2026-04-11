package com.hoz.hozitech.application.services.coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

public interface CouponService {
    // Admin
    PageResponse<CouponResponse> getAllCoupons(String keyword, int page, int size);
    CouponResponse getCouponById(UUID id);
    CouponResponse getCouponByCode(String code);
    CouponResponse createCoupon(CouponRequest request);
    CouponResponse updateCoupon(UUID id, CouponRequest request);
    CouponResponse toggleStatus(UUID id);

    // User - public vouchers
    List<CouponResponse> getPublicCoupons(UUID userId);

    // User - save/unsave voucher
    void saveCoupon(UUID userId, UUID couponId);
    void unsaveCoupon(UUID userId, UUID couponId);
    List<CouponResponse> getMySavedCoupons(UUID userId);

    // User - validate
    CouponResponse validateCoupon(String code, BigDecimal orderAmount);
}
