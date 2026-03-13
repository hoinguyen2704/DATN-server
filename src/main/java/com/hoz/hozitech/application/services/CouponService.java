package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface CouponService {
    // Admin
    PageResponse<CouponResponse> getAllCoupons(String keyword, int page, int size);

    CouponResponse getCouponById(UUID id);

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(UUID id, CouponRequest request);

    CouponResponse toggleStatus(UUID id);

    // User
    CouponResponse validateCoupon(String code, BigDecimal orderAmount);
}
