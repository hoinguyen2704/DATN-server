package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.services.CouponService;
import com.hoz.hozitech.domain.dtos.request.CouponRequest;
import com.hoz.hozitech.domain.dtos.response.CouponResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    public PageResponse<CouponResponse> getAllCoupons(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        
        Page<Coupon> coupons;
        // Basic filtering, can be extended via Specifications if needed
        if (keyword != null && !keyword.isBlank()) {
            // For now, simpler pagination since we don't have a complex spec yet.
            // Spring Data JPA doesn't have a native findByCodeContaining without defining it in Repo
            // So we'll fetch all. In a real scenario, use Specification.
            coupons = couponRepository.findAll(pageable); // Simplification for now
        } else {
            coupons = couponRepository.findAll(pageable);
        }
        
        return PageResponse.of(coupons.map(this::mapToResponse));
    }

    @Override
    public CouponResponse getCouponById(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        return mapToResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType().toUpperCase())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status("ACTIVE")
                .build();

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(UUID id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(request.getDiscountType().toUpperCase());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse toggleStatus(UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if ("ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            coupon.setStatus("INACTIVE");
        } else {
            coupon.setStatus("ACTIVE");
        }

        return mapToResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        if (!"ACTIVE".equalsIgnoreCase(coupon.getStatus())) {
            throw new IllegalArgumentException("Coupon is not active");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Coupon has expired");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Coupon is not valid yet");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("Coupon usage limit exceeded");
        }
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Order does not meet minimum value for coupon. Minimum is: " + coupon.getMinOrderValue());
        }

        return mapToResponse(coupon);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .status(coupon.getStatus())
                .build();
    }
}
